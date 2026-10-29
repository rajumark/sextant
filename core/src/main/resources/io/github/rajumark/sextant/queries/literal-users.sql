-- Every instruction that loads one literal number, with its method and offset: magic numbers, ports, flags, resource ids. Param: value (decimal).
SELECT method, "offset", op
FROM instruction_view
WHERE literal = CAST(:value AS INTEGER)
ORDER BY method, "offset";
