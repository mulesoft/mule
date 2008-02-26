<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

    <xsl:param name="echo"/>

    <xsl:template match="/">
        <echo-value><xsl:value-of select="$echo"/></echo-value>
    </xsl:template>
</xsl:stylesheet>
