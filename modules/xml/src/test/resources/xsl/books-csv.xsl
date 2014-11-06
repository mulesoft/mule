<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">

<xsl:output method="text"/>

<!-- This stylesheet outputs the book list as a CSV file -->

<xsl:template match="BOOKLIST">
        <xsl:apply-templates select="BOOKS"/>
</xsl:template>

<xsl:template match="BOOKS">
<xsl:text>Title,Author,Category,Stock-Value</xsl:text>
<xsl:for-each select="ITEM">
"<xsl:value-of select="TITLE"/>","<xsl:text/>
<xsl:value-of select="AUTHOR"/>","<xsl:text/>
<xsl:value-of select="@CAT"/>(<xsl:text/>
        <xsl:choose>
        <xsl:when test='@CAT="F"'>Fiction</xsl:when>
        <xsl:when test='@CAT="S"'>Science</xsl:when>
        <xsl:when test='@CAT="C"'>Computing</xsl:when>
        <xsl:when test='@CAT="X"'>Crime</xsl:when>
        <xsl:otherwise>Unclassified</xsl:otherwise>
        </xsl:choose>)","<xsl:text/>
<xsl:value-of select="if (@VALUE) then format-number(@VALUE,'######0.00') else 'N/A'"/>"<xsl:text/>
</xsl:for-each><xsl:text>
</xsl:text>
</xsl:template>

</xsl:stylesheet>	
