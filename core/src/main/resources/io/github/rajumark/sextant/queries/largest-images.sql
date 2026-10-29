-- Images by pixel area, largest first, with their file size: candidates for downscaling or WebP.
SELECT i.path, i.format, i.width, i.height, i.width * i.height AS pixels, f.size_bytes
FROM images i
JOIN files f ON f.path = i.path
WHERE i.width IS NOT NULL
ORDER BY pixels DESC, f.size_bytes DESC;
