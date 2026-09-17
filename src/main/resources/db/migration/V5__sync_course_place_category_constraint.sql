ALTER TABLE course_places
    DROP CONSTRAINT IF EXISTS course_places_category_check;

UPDATE course_places
SET category = 'FOOD'
WHERE category = 'FOOD_CAFE';

ALTER TABLE course_places
    ADD CONSTRAINT course_places_category_check
        CHECK (category IN (
            'FOOD',
            'CAFE',
            'TOUR_CULTURE',
            'PARK_TRAIL',
            'LODGING',
            'TRANSPORTATION',
            'PUBLIC_FACILITY',
            'ETC'
        ));
