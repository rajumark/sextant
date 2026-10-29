-- Kotlin classes by the kind their @kotlin.Metadata declares (1 class, 2 file facade, 3 synthetic, 4 multi-file facade, 5 multi-file part).
SELECT CASE kind WHEN '1' THEN 'class' WHEN '2' THEN 'file facade' WHEN '3' THEN 'synthetic class'
                 WHEN '4' THEN 'multi-file facade' WHEN '5' THEN 'multi-file part' ELSE kind END AS kind,
       count(*) AS classes
FROM kotlin_classes
GROUP BY kind
ORDER BY classes DESC;
