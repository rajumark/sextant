-- Who signed the app, per scheme: the SDK range each signer covers, its certificate subject, key, validity and fingerprint.
SELECT s.scheme, s.min_sdk, s.max_sdk, c.subject, c.key_algorithm || ' ' || c.key_size AS key,
       c.signature_algorithm, c.not_after, c.sha256
FROM signing_signers s
LEFT JOIN signing_certificates c ON c.sha256 = s.certificate
ORDER BY s.scheme, s.idx;
