-- Marcador visual escolhido pela própria pessoa. Não é diagnóstico.
ALTER TABLE ss_accessibility_preferences
    ADD COLUMN IF NOT EXISTS accessibility_symbol VARCHAR(24);

ALTER TABLE ss_accessibility_preferences
    DROP CONSTRAINT IF EXISTS ss_accessibility_preferences_accessibility_symbol_check;

ALTER TABLE ss_accessibility_preferences
    ADD CONSTRAINT ss_accessibility_preferences_accessibility_symbol_check
    CHECK (accessibility_symbol IS NULL OR accessibility_symbol IN
        ('blue-triangle', 'orange-square', 'white-circle'));
