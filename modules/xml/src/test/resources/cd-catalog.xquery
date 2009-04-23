declare variable $document external;
<cd-listings> {
    for $cd in $document/catalog/cd
    return <cd-title>{data($cd/title)}</cd-title>
} </cd-listings>