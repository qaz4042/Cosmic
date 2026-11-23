--liquibase formatted sql

--changeset maple:20261123-starter-defaults
ALTER TABLE characters ALTER COLUMN equipslots SET DEFAULT 96;
ALTER TABLE characters ALTER COLUMN useslots SET DEFAULT 96;
ALTER TABLE characters ALTER COLUMN setupslots SET DEFAULT 96;
ALTER TABLE characters ALTER COLUMN etcslots SET DEFAULT 96;
ALTER TABLE accounts ALTER COLUMN nxCredit SET DEFAULT 1000000;

--changeset maple:20261123-starter-backfill
-- One-time top-up: preserve larger inventories and balances, and do not
-- refill currency on later logins or server restarts.
UPDATE characters SET
    equipslots = GREATEST(equipslots, 96),
    useslots = GREATEST(useslots, 96),
    setupslots = GREATEST(setupslots, 96),
    etcslots = GREATEST(etcslots, 96);
UPDATE accounts SET nxCredit = GREATEST(COALESCE(nxCredit, 0), 1000000);
