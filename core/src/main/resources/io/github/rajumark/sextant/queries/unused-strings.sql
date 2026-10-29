-- Strings in the default configuration that nothing references: no layout, menu, style, manifest entry or code.
SELECT name, value
FROM string_resources s
WHERE config = '' AND kind = 'string'
  AND NOT EXISTS (SELECT 1 FROM resource_refs r WHERE r.ref = '@string/' || s.name)
  AND NOT EXISTS (SELECT 1 FROM xml_attrs a WHERE a.ref = '@string/' || s.name)
ORDER BY name;
