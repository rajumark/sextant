-- Dangerous (runtime) permissions the app requests, including ones Android grants implicitly.
SELECT name, kind, max_sdk, implied_by
FROM permissions
WHERE dangerous = 1 AND kind != 'declared'
ORDER BY name;
