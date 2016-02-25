<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">
    <xsl:output method="xml"/>

    <xsl:param name="vtn" />
    <xsl:template match="/parameter">
        <param><xsl:value-of select="$vtn"/></param>
    </xsl:template>
</xsl:stylesheet>