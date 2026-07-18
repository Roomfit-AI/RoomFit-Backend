-- MUTATING production cleanup. Run only after reviewing the separate dry-run.
-- The transaction aborts unless room_id=2 is the one exact legacy SAMPLE.
BEGIN;

DO $$
DECLARE
    candidate_count integer;
BEGIN
    SELECT COUNT(*) INTO candidate_count
    FROM room
    WHERE id = 2
      AND source = 'SAMPLE'
      AND name = '샘플룸2'
      AND width = 6.4
      AND depth = 5.8;

    IF candidate_count <> 1 THEN
        RAISE EXCEPTION 'Cleanup aborted: expected exactly one legacy SAMPLE room, found %', candidate_count;
    END IF;
END $$;

DELETE FROM layout_furniture
WHERE layout_id IN (SELECT id FROM layout WHERE room_id = 2);
DELETE FROM layout WHERE room_id = 2;

DELETE FROM agent_context_design_style
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context_required_items
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context_optional_items
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context_selected_image_ids
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context_selected_product_ids
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context_style_tags
WHERE context_id IN (SELECT id FROM agent_context WHERE room_id = 2);
DELETE FROM agent_context WHERE room_id = 2;

DELETE FROM room_import_warnings WHERE room_id = 2;
DELETE FROM room_furniture WHERE room_id = 2;
DELETE FROM room_openings WHERE room_id = 2;
DELETE FROM room_walls WHERE room_id = 2;
DELETE FROM room
WHERE id = 2
  AND source = 'SAMPLE'
  AND name = '샘플룸2'
  AND width = 6.4
  AND depth = 5.8;

COMMIT;
