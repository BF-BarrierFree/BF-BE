ALTER TABLE terms
    ADD COLUMN term_key VARCHAR(100);

UPDATE terms
SET term_key = title
WHERE term_key IS NULL;

WITH revisions AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY term_key
               ORDER BY version, effective_date, id
           )::INTEGER AS normalized_version
    FROM terms
)
UPDATE terms
SET version = revisions.normalized_version
FROM revisions
WHERE terms.id = revisions.id;

WITH active_revisions AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY term_key
               ORDER BY version DESC, effective_date DESC, id DESC
           ) AS active_rank
    FROM terms
    WHERE is_active = TRUE
)
UPDATE terms
SET is_active = FALSE
FROM active_revisions
WHERE terms.id = active_revisions.id
  AND active_revisions.active_rank > 1;

ALTER TABLE terms
    ALTER COLUMN term_key SET NOT NULL,
    ADD CONSTRAINT uk_terms_term_key_version UNIQUE (term_key, version);

CREATE UNIQUE INDEX uk_terms_active_term_key
    ON terms (term_key)
    WHERE is_active = TRUE;

WITH duplicate_agreements AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, term_id
               ORDER BY updated_at DESC, id DESC
           ) AS agreement_rank
    FROM user_term_agreements
)
DELETE FROM user_term_agreements
USING duplicate_agreements
WHERE user_term_agreements.id = duplicate_agreements.id
  AND duplicate_agreements.agreement_rank > 1;

ALTER TABLE user_term_agreements
    ADD CONSTRAINT uk_user_term_agreements_user_term UNIQUE (user_id, term_id);
