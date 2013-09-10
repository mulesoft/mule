<?xml version="1.0"?>
<!--

    Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
    The software in this package is published under the terms of the CPAL v1.0
    license, a copy of which has been included with this distribution in the
    LICENSE.txt file.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <!-- From the XSLT spec: "the identity transformation can be written using xsl:copy as follows:" -->
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>
</xsl:stylesheet>

