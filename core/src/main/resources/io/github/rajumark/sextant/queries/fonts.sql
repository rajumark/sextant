-- Every font the app ships, with the family, style and version from its name table.
SELECT path, family, subfamily, version, postscript_name
FROM fonts
ORDER BY family, subfamily;
