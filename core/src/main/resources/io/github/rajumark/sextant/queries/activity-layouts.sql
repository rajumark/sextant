-- Which layouts each screen shows: set as content view or inflated by its own code, or through a generated *Binding class.
SELECT substr(src, length('component:') + 1) AS activity, dst AS layout, via
FROM links
WHERE rel = 'shows'
ORDER BY activity, layout;
