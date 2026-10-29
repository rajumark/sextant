-- Network hosts the app refers to, with how many URLs and where they were found (code, resource, native, …).
SELECT e.host, count(DISTINCT e.url) AS urls, group_concat(DISTINCT s.kind) AS found_in
FROM endpoints e
JOIN endpoint_sources s ON s.endpoint = e.id
WHERE e.scheme IN ('http', 'https', 'ws', 'wss')
GROUP BY e.host
ORDER BY urls DESC, e.host;
