<?xml version="1.0" encoding="ISO-8859-1"?>
<!--

    Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
    The software in this package is published under the terms of the CPAL v1.0
    license, a copy of which has been included with this distribution in the
    LICENSE.txt file.

-->
<!-- Edited with XML Spy v4.2 -->
<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml"/>
<xsl:include href="./cdcatalog-echo.xsl"/>
<xsl:template match="/">

  <html>
  <body>
  <h2>My CD Collection</h2>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th align="left">Title</th>
        <th align="left">Artist</th>
      </tr>
      <xsl:for-each select="catalog/cd">
      <tr>
        <td><xsl:value-of select="title"/></td>
        <td><xsl:value-of select="artist"/></td>
      </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>
