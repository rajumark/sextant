-- Every ELF file in the APK: architecture, soname, the libraries it needs, and its exported, imported and string counts.
SELECT e.path, e.machine, e.soname,
       (SELECT group_concat(needed, ' ') FROM elf_needed n WHERE n.path = e.path) AS needs,
       e.exported, e.imported, e.strings
FROM elf_files e
ORDER BY e.path;
