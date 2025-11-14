ALTER TABLE patient_files
    DROP COLUMN IF EXISTS health_insurance_card_number,
    DROP COLUMN IF EXISTS health_insurance_code;
