-- Read-only production dry-run for the one known non-canonical public sample.
-- This file contains no mutation. Verify every result before running
-- public-sample-cleanup-execute.sql separately.

-- DRY-RUN
SELECT id, name, source, width, depth, height, client_scope
FROM room
WHERE source = 'SAMPLE'
ORDER BY id;

SELECT COUNT(*) AS delete_candidate_count
FROM room
WHERE id = 2
  AND source = 'SAMPLE'
  AND name = '샘플룸2'
  AND width = 6.4
  AND depth = 5.8;

SELECT COUNT(*) AS furniture_count FROM room_furniture WHERE room_id = 2;
SELECT COUNT(*) AS opening_count FROM room_openings WHERE room_id = 2;
SELECT COUNT(*) AS wall_count FROM room_walls WHERE room_id = 2;
SELECT COUNT(*) AS import_warning_count FROM room_import_warnings WHERE room_id = 2;
SELECT COUNT(*) AS layout_count FROM layout WHERE room_id = 2;
SELECT COUNT(*) AS agent_context_count FROM agent_context WHERE room_id = 2;
