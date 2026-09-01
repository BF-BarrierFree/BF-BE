CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    is_ai_generated BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_courses_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_course_user_id ON courses (user_id);

CREATE TABLE IF NOT EXISTS course_places (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    sequence INTEGER NOT NULL,
    original_place_id VARCHAR(150),
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    photo_url VARCHAR(1000),
    distance_to_next VARCHAR(50),
    moving_tip VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_course_places_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);
