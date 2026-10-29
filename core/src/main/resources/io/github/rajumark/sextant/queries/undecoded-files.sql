-- Files inside the APK that no decoder turned into rows, largest first.
SELECT type, path, size_bytes, magic
FROM files
WHERE decoded_by IS NULL
ORDER BY size_bytes DESC;
