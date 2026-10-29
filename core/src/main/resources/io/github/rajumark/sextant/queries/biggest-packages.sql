-- Packages by number of classes, with how many methods they define.
SELECT c.package, count(*) AS classes, sum(c.method_count) AS methods
FROM classes c
GROUP BY c.package
ORDER BY classes DESC;
