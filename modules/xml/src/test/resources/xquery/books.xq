xquery version "3.0";
declare copy-namespaces no-preserve, inherit;
declare variable $document external;

for $b in $document//BOOKS/ITEM
order by string-length($b/TITLE) return
<book>
  <author> { $b/AUTHOR } </author>
  <title> { $b/TITLE } </title>
</book>