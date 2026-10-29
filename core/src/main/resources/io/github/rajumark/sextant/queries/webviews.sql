-- Where WebViews are used: declared in layouts, or loaded and given JavaScript bridges in code.
SELECT 'layout' AS kind, f.resource AS location, n.tag AS detail
FROM xml_nodes n JOIN files f ON f.id = n.file
WHERE n.tag LIKE '%WebView'
UNION ALL
SELECT 'code', caller, substr(callee, instr(callee, '->') + 2)
FROM call_edges
WHERE callee LIKE 'Landroid/webkit/WebView;->loadUrl%'
   OR callee LIKE 'Landroid/webkit/WebView;->addJavascriptInterface%'
   OR callee LIKE 'Landroid/webkit/WebSettings;->setJavaScriptEnabled%'
ORDER BY kind, location;
