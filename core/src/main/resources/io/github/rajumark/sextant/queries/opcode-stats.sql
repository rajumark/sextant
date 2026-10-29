-- How often each Dalvik opcode is used in the app's code.
SELECT o.name AS opcode, count(*) AS uses
FROM instructions i
JOIN opcodes o ON o.id = i.op
GROUP BY o.name
ORDER BY uses DESC;
