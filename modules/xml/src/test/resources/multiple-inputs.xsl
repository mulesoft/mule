<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">

    <xsl:output method="xml" />

    <xsl:param name="f2" select="'cities.xml'"/>
    <xsl:param name="f3" select="'books.xml'"/>

    <xsl:variable name="doc2" select="document($f2)"/>
    <xsl:variable name="doc3" select="document($f3)"/>

    <xsl:key name="k1" match="Response/Result/info" use="id"/>

    <xsl:template match="/">
        <xsl:copy-of select="$doc2" />
        <xsl:copy-of select="$doc3" />
    </xsl:template>

</xsl:stylesheet>