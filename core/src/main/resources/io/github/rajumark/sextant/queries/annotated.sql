-- Everything carrying one annotation, with its values. Param: type, a descriptor such as Ljavax/inject/Inject; or Ljava/lang/Deprecated;
SELECT a.target_kind, a.target, a.parameter, group_concat(v.name || '=' || v.value, ', ') AS "values"
FROM annotations a
LEFT JOIN annotation_values v ON v.annotation = a.id
WHERE a.type = :type
GROUP BY a.id
ORDER BY a.target;
