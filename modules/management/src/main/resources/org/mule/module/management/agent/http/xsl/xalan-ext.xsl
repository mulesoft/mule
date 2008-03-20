<?xml version="1.0"?>
<!--
 $Id$
 
 Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 
 The software in this package is published under the terms of the CPAL v1.0
 license, a copy of which has been included with this distribution in the
 LICENSE.txt file.
 -->
<!--
 Copyright (C) The MX4J Contributors.
 All rights reserved.

 This software is distributed under the terms of the MX4J License version 1.0.
 See the terms of the MX4J License in the documentation provided with this software.

 Author: Carlos Quiroz (tibu@users.sourceforge.net)
                 Brett Knights
 Revision: 1.2
-->

<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="1.0"
   xmlns:encoder="/java.net.URLEncoder"
   xmlns:java="http://xml.apache.org/xslt/java"
   exclude-result-prefixes="java">

   <xsl:template name="uri-encode">
      <xsl:param name="uri"/>
      <xsl:choose>
         <xsl:when test="function-available('java:java.net.URLEncoder.encode')">
            <xsl:value-of select="java:java.net.URLEncoder.encode($uri)"/>
         </xsl:when>
         <xsl:when test="function-available('encoder:encode')">
            <xsl:value-of select="encoder:encode($uri)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No encode function available</xsl:message>
            <xsl:value-of select="$uri"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
</xsl:stylesheet>

