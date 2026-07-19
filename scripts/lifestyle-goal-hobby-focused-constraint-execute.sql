-- Run lifestyle-goal-hobby-focused-constraint-check.sql first and confirm
-- unexpected_value_count = 0 before running this.
--
-- Drops and recreates the CHECK constraint on agent_context.lifestyle_goal
-- so it accepts the new HOBBY_FOCUSED value (see LifestyleGoal.java on
-- improve/recommendation-quality). Uses the constraint name discovered by
-- the check script dynamically -- do not hardcode a guessed name here,
-- Hibernate/Postgres naming isn't guaranteed across environments.
--
-- Apply this as part of deploying improve/recommendation-quality, before
-- (or in the same release window as) turning on any code path that can
-- send lifestyleGoal=HOBBY_FOCUSED. Take a snapshot/backup first per your
-- normal deploy process -- this is a one-time, low-risk, additive
-- constraint change (widening an allowed-values list, not narrowing one),
-- but it's still a manual production DDL change.

DO $$
DECLARE
    existing_constraint_name text;
BEGIN
    SELECT con.conname INTO existing_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
    WHERE rel.relname = 'agent_context'
      AND att.attname = 'lifestyle_goal'
      AND con.contype = 'c';

    IF existing_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE agent_context DROP CONSTRAINT %I', existing_constraint_name);
    END IF;

    ALTER TABLE agent_context
        ADD CONSTRAINT agent_context_lifestyle_goal_check
        CHECK (lifestyle_goal IN (
            'RELAX_FOCUSED', 'STORAGE_FOCUSED', 'STUDY_FOCUSED', 'WFH_FOCUSED', 'HOBBY_FOCUSED'
        ));
END $$;

-- Verify: should now show the 5-value constraint.
SELECT con.conname AS constraint_name,
       pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
WHERE rel.relname = 'agent_context'
  AND att.attname = 'lifestyle_goal'
  AND con.contype = 'c';
