-- Which layouts each activity's own code inflates or sets as content view.
SELECT c.name AS activity, r.ref AS layout, substr(r.attribute, instr(r.attribute, '->') + 2) AS via
FROM components c
JOIN methods m ON m.class = 'L' || replace(c.name, '.', '/') || ';'
JOIN resource_refs r ON r.from_kind = 'code' AND r."from" = m.descriptor AND r.ref LIKE '@layout/%'
WHERE c.type = 'activity'
GROUP BY c.name, r.ref
ORDER BY c.name, r.ref;
