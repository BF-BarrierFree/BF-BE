-- 기존 리뷰는 지역 정보가 없어 NULL로 유지합니다. 검증된 장소 정보로 별도 보완해야 합니다.
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS region VARCHAR(100);
-- Hibernate ddl-auto=update는 기존 NOT NULL 평점 컬럼을 제거하지 않으므로 반드시 적용합니다.
ALTER TABLE reviews DROP COLUMN IF EXISTS rating;
CREATE INDEX IF NOT EXISTS idx_review_region_active ON reviews (region) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_review_helpfuls_review_id ON review_helpfuls (review_id);
