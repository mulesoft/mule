<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
<xsl:output method="html"/>
<xsl:template match="/">
<p class="example-title">MuleForge Examples</p>
<ul class="example-text">
<xsl:for-each select="muleforge-extensions/muleforge-extension">
    <li>
                <a>
<xsl:attribute name="href">javascript:downloadAndDeployExample('<xsl:value-of select="download-url" />')</xsl:attribute>
                <xsl:value-of select="title"/>
                </a>
    </li>
</xsl:for-each>
</ul>
</xsl:template>
</xsl:stylesheet>
