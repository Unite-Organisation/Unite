DELETE FROM post WHERE area_id IS NOT NULL;

ALTER TABLE post DROP CONSTRAINT area_or_building_not_both_null_or_not_null_ann;
ALTER TABLE post ALTER COLUMN building_id SET NOT NULL;
ALTER TABLE post DROP COLUMN area_id;

CREATE INDEX idx_post_building_created ON post (building_id, created_at);
CREATE UNIQUE INDEX idx_building_manager_user_building ON building_manager (user_id, building_id);
