<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:template name="echo">
        <xsl:param name="p0"/>
        <xsl:value-of select="$p0"/>
    </xsl:template>
</xsl:stylesheet>
