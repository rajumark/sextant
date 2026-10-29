-- Methods in the app's own code that catch Throwable or Exception, a common way errors get swallowed. Library packages are left out.
SELECT m.descriptor AS method, h.type AS catches, count(*) AS handlers
FROM exception_handlers h
JOIN methods m ON m.id = h.method
WHERE h.type IN ('Ljava/lang/Throwable;', 'Ljava/lang/Exception;')
  AND m.class NOT LIKE 'Landroidx/%' AND m.class NOT LIKE 'Lkotlin%' AND m.class NOT LIKE 'Lcom/google/%'
  AND m.class NOT LIKE 'Lokhttp3/%' AND m.class NOT LIKE 'Ljava/%'
GROUP BY m.descriptor, h.type
ORDER BY handlers DESC, method;
