-- Read-only production dry-run. This file contains no mutation.
--
-- Context: LifestyleGoal.java gained a new constant, HOBBY_FOCUSED
-- (see improve/recommendation-quality). Hibernate's ddl-auto: update does
-- NOT alter an existing CHECK constraint on an @Enumerated(EnumType.STRING)
-- column when the enum's value set changes -- it only adds new
-- tables/columns. The constraint Hibernate generated when agent_context was
-- first created still only allows the original 4 values, so any request
-- with lifestyleGoal=HOBBY_FOCUSED will fail at the database with something
-- like:
--   ERROR: new row for relation "agent_context" violates check constraint
-- This was reproduced locally: a fresh H2 database accepts HOBBY_FOCUSED
-- fine (constraint generated fresh with all 5 values), but an existing
-- database created before this change rejects it.
--
-- Run this first and inspect the output before running
-- lifestyle-goal-hobby-focused-constraint-execute.sql.

-- Find the actual constraint name and definition on this database --
-- Hibernate's auto-generated name may not match what you'd guess.
SELECT con.conname AS constraint_name,
       pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
WHERE rel.relname = 'agent_context'
  AND att.attname = 'lifestyle_goal'
  AND con.contype = 'c';

-- Confirm no existing row already has a value the new constraint would
-- reject (sanity check -- should always return 0 since HOBBY_FOCUSED is a
-- brand new value nothing could have written yet).
SELECT COUNT(*) AS unexpected_value_count
FROM agent_context
WHERE lifestyle_goal IS NOT NULL
  AND lifestyle_goal NOT IN ('RELAX_FOCUSED', 'STORAGE_FOCUSED', 'STUDY_FOCUSED', 'WFH_FOCUSED');
