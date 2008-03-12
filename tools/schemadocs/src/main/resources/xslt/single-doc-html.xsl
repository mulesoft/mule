<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <xsl:output method="html"/>
    <xsl:variable name="transport" select="'poop'"/>

    <xsl:template match="/">
        <html>
            <body>
                poop
            </body>
        </html>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-menu-connector"><xsl:variable name="textname" select="translate(@name, '-', ' ')"/>
* [<xsl:value-of select="substring($transport, 1, 1)"/><xsl:value-of select="substring($transport, 2)"/> connector|#<xsl:value-of select="$transport"/>-<xsl:value-of select="@name"/>]</xsl:template>

</xsl:stylesheet>
