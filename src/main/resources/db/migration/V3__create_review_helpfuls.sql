CREATE TABLE IF NOT EXISTS review_helpfuls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_review_helpfuls_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_helpfuls_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT uk_review_helpful_user_review UNIQUE (user_id, review_id)
);

CREATE INDEX IF NOT EXISTS idx_review_helpfuls_user_id ON review_helpfuls (user_id);
