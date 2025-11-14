CREATE TEMP TABLE tmp_duplicate_users AS
WITH duplicates AS (
    SELECT LOWER(TRIM(email)) AS normalized_email,
           ARRAY_AGG(id ORDER BY created_at, id) AS all_ids
    FROM users
    GROUP BY LOWER(TRIM(email))
    HAVING COUNT(*) > 1
)
SELECT normalized_email,
       all_ids[1] AS canonical_id,
       all_ids
FROM duplicates;

-- Expand duplicates (skip canonical entry).
CREATE TEMP TABLE tmp_duplicate_members AS
SELECT normalized_email,
       canonical_id,
       unnest(all_ids[2:array_length(all_ids, 1)]) AS duplicate_id
FROM tmp_duplicate_users
WHERE array_length(all_ids, 1) > 1;

-- Reassign foreign keys referencing duplicate users to the canonical user.
DO $$
DECLARE
    dup RECORD;
    fk  RECORD;
BEGIN
    FOR dup IN SELECT * FROM tmp_duplicate_members LOOP
        -- Update every FK that references users(id).
        FOR fk IN
            SELECT tc.table_schema,
                   tc.table_name,
                   kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage ccu
              ON ccu.constraint_name = tc.constraint_name
             AND ccu.table_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND ccu.table_schema = 'public'
              AND ccu.table_name = 'users'
              AND ccu.column_name = 'id'
        LOOP
            BEGIN
                EXECUTE format(
                    'UPDATE %I.%I SET %I = $1 WHERE %I = $2',
                    fk.table_schema,
                    fk.table_name,
                    fk.column_name,
                    fk.column_name
                )
                USING dup.canonical_id, dup.duplicate_id;
            EXCEPTION WHEN unique_violation THEN
                -- If the update violates a unique constraint, remove the duplicate row.
                EXECUTE format(
                    'DELETE FROM %I.%I WHERE %I = $1',
                    fk.table_schema,
                    fk.table_name,
                    fk.column_name
                )
                USING dup.duplicate_id;
            END;
        END LOOP;

        -- Remove the duplicate user after reassigning references.
        DELETE FROM users WHERE id = dup.duplicate_id;
    END LOOP;
END $$;

-- Normalize remaining user e-mails.
UPDATE users
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL AND email <> LOWER(TRIM(email));

-- Normalize secretary corporate e-mails stored in invitations.
UPDATE team_invites
SET secretary_corporate_email = LOWER(TRIM(secretary_corporate_email))
WHERE secretary_corporate_email IS NOT NULL
  AND secretary_corporate_email <> LOWER(TRIM(secretary_corporate_email));

-- Enforce case-insensitive uniqueness for user e-mails.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (LOWER(email));

DROP TABLE IF EXISTS tmp_duplicate_members;
DROP TABLE IF EXISTS tmp_duplicate_users;
