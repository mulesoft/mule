<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

    <xsl:output method="text" />

    <xsl:param name="output_location" />

    <xsl:template match="/">
        <xsl:result-document href="{$output_location}" >
            <xsl:for-each select="cities/city"><xsl:value-of select="@country"/> - <xsl:value-of select="@name"/> - <xsl:value-of select="@pop"/> | </xsl:for-each>
        </xsl:result-document>
    </xsl:template>
</xsl:stylesheet>