-- Service-loader registrations (META-INF/services): which class the app registers for which interface.
SELECT interface, implementation, path
FROM services
ORDER BY interface, implementation;
