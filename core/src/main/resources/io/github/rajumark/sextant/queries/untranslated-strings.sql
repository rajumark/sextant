-- App strings that are missing in some of the locales most of the app is translated into.
WITH app_locales AS (
  SELECT locale FROM string_resources
  WHERE locale IS NOT NULL AND kind = 'string'
  GROUP BY locale
  HAVING count(DISTINCT name) * 2 >= (SELECT count(DISTINCT name) FROM string_resources WHERE locale IS NOT NULL AND kind = 'string')
)
SELECT s.name, (SELECT count(*) FROM app_locales) - count(DISTINCT s.locale) AS missing_locales
FROM string_resources s
WHERE s.kind = 'string' AND s.locale IN (SELECT locale FROM app_locales)
GROUP BY s.name
HAVING missing_locales > 0
ORDER BY missing_locales DESC, s.name;
