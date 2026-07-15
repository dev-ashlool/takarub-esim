-- SEO-stable country slugs for public catalog URLs.

ALTER TABLE catalog_countries
    ADD COLUMN slug VARCHAR(255) NULL;

UPDATE catalog_countries
SET slug = LOWER(REPLACE(TRIM(english_name), ' ', '-'))
WHERE english_name IS NOT NULL
  AND TRIM(english_name) <> '';

UPDATE catalog_countries
SET slug = LOWER(id)
WHERE slug IS NULL OR slug = '' OR slug = '-';

-- Disambiguate collisions by appending the location id.
UPDATE catalog_countries c
INNER JOIN (
    SELECT slug
    FROM catalog_countries
    GROUP BY slug
    HAVING COUNT(*) > 1
) d ON c.slug = d.slug
SET c.slug = CONCAT(c.slug, '-', LOWER(REPLACE(REPLACE(c.id, ' ', '-'), '_', '-')));

ALTER TABLE catalog_countries
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX uq_catalog_countries_slug ON catalog_countries (slug);
