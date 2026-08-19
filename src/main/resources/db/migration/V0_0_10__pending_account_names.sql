-- An invited account is created from an e-mail address alone, so the name is only known once the
-- invited person follows their activation link and fills it in.
ALTER TABLE app_user ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE app_user ALTER COLUMN last_name DROP NOT NULL;
