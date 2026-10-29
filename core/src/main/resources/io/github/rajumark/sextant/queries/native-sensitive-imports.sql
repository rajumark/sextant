-- Native code that imports functions worth a look in a review: code loading, process control, memory protection, anti-debugging.
SELECT name AS function, group_concat(DISTINCT path) AS libraries
FROM elf_symbols
WHERE defined = 0 AND name IN ('dlopen', 'dlsym', 'android_dlopen_ext', 'execve', 'execv', 'execvp', 'system', 'popen',
  'fork', 'vfork', 'ptrace', 'mprotect', 'mmap', '__system_property_get', 'syscall', 'inotify_add_watch')
GROUP BY name
ORDER BY name;
