-- Java methods implemented in native code, with the library that implements them.
SELECT jni_class, jni_method, library
FROM native_symbols
WHERE jni_class IS NOT NULL
ORDER BY jni_class, jni_method;
