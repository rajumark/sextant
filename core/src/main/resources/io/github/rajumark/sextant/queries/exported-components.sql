-- Components any app can reach: exported, enabled and not protected by a permission, with their deep links.
SELECT c.type, c.name, c.exported_source,
       (SELECT group_concat(url, ' ') FROM deep_links d WHERE d.component = c.name) AS deep_links
FROM components c
WHERE c.exported = 1 AND c.enabled = 1 AND c.permission IS NULL
ORDER BY c.type, c.name;
