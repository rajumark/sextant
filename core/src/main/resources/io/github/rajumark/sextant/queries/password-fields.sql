-- Every password input field declared in a layout, with the layout and the view class.
SELECT f.resource AS layout, n.tag AS view, a.value AS input_type, n.path
FROM xml_nodes n
JOIN xml_attrs a ON a.node = n.id
JOIN files f ON f.id = n.file
WHERE a.name = 'android:inputType' AND a.value LIKE '%assword%'
ORDER BY layout;
