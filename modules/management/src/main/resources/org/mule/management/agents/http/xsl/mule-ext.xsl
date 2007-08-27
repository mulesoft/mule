<?xml version="1.0"?>
<!--
 $Id$
 
 Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 
 The software in this package is published under the terms of the CPAL v1.0
 license, a copy of which has been included with this distribution in the
 LICENSE.txt file.
 -->

<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="1.0"
   xmlns:java="http://xml.apache.org/xslt/java"
   xmlns:mulemanager="xalan://org.mule.MuleManager"
   xmlns:muleconfiguration="xalan://org.mule.config.MuleConfiguration"
   exclude-result-prefixes="java">

   <xsl:template name="buildDate">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getBuildDate($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="encoding">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getEncoding($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="model">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getModel($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="modelType">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getModelType($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="osEncoding">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getOSEncoding($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productDescription">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductDescription($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productLicenseInfo">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductLicenseInfo($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productMoreInfo">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductMoreInfo($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productName">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductName($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productSupport">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductSupport($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productUrl">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductUrl($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productVersion">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getProductVersion($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="serverUrl">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getServerUrl($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="systemModelType">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getSystemModelType($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="vendorName">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getVendorName($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="vendorUrl">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getVendorUrl($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="workingDirectory">
      <xsl:choose>
         <xsl:when test="function-available('mulemanager:getConfiguration')">
            <xsl:variable name="configuration" select="mulemanager:getConfiguration()"/>
            <xsl:value-of select="muleconfiguration:getWorkingDirectory($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
</xsl:stylesheet>

