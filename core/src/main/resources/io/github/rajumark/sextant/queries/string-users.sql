-- Everything that uses one string resource: files (layouts, menus, …), the manifest and methods. Param: name.
SELECT src AS used_by, via
FROM links
WHERE dst = '@string/' || :name AND rel = 'uses'
ORDER BY src;
