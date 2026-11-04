-- Dex code the app carries outside its own classesN.dex (nested jars, zips, APKs or loose dex files): code it can load at run time.
SELECT container, entry, count(*) AS classes, sum(methods) AS methods,
       min(descriptor) AS first_class
FROM hidden_classes
GROUP BY container, entry
ORDER BY classes DESC;
