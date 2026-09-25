-- Preferred UI language (tr, en, zh, …) so the choice follows the user across devices.
ALTER TABLE users ADD COLUMN language VARCHAR(8);
