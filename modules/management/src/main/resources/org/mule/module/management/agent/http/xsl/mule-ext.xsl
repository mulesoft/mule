<?xml version="1.0"?>
<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="1.0"
   xmlns:java="http://xml.apache.org/xslt/java"
   xmlns:mulemanifest="xalan://org.mule.config.MuleManifest"
   xmlns:muleserver="xalan://org.mule.MuleServer"
   xmlns:mulecontext="xalan://org.mule.api.MuleContext"
   xmlns:configuration="xalan://org.mule.config.MuleConfiguration"
   exclude-result-prefixes="java">

   <xsl:template name="buildDate">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getBuildDate')">
            <xsl:value-of select="mulemanifest:getBuildDate()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="encoding">
      <xsl:choose>
         <xsl:when test="function-available('muleserver:getMuleContext')">
            <xsl:variable name="mulecontext" select="muleserver:getMuleContext()"/>
            <xsl:variable name="configuration" select="mulecontext:getConfiguration($mulecontext)"/>
            <xsl:value-of select="configuration:getDefaultEncoding($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productDescription">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductDescription')">
            <xsl:value-of select="mulemanifest:getProductDescription()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productLicenseInfo">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductLicenseInfo')">
            <xsl:value-of select="mulemanifest:getProductLicenseInfo()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productMoreInfo">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductMoreInfo')">
            <xsl:value-of select="mulemanifest:getProductMoreInfo()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productName">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductName')">
            <xsl:value-of select="mulemanifest:getProductName()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productSupport">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductSupport')">
            <xsl:value-of select="mulemanifest:getProductSupport()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productUrl">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductUrl')">
            <xsl:value-of select="mulemanifest:getProductUrl()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="productVersion">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getProductVersion')">
            <xsl:value-of select="mulemanifest:getProductVersion()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="systemModelType">
      <xsl:choose>
         <xsl:when test="function-available('muleserver:getMuleContext')">
            <xsl:variable name="mulecontext" select="muleserver:getMuleContext()"/>
            <xsl:variable name="configuration" select="mulecontext:getConfiguration($mulecontext)"/>
            <xsl:value-of select="configuration:getSystemModelType($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="vendorName">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getVendorName')">
            <xsl:value-of select="mulemanifest:getVendorName()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="vendorUrl">
      <xsl:choose>
         <xsl:when test="function-available('mulemanifest:getVendorUrl')">
            <xsl:value-of select="mulemanifest:getVendorUrl()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="workingDirectory">
      <xsl:choose>
         <xsl:when test="function-available('muleserver:getMuleContext')">
            <xsl:variable name="mulecontext" select="muleserver:getMuleContext()"/>
            <xsl:variable name="configuration" select="mulecontext:getConfiguration($mulecontext)"/>
            <xsl:value-of select="configuration:getWorkingDirectory($configuration)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:message>No configuration available</xsl:message>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
</xsl:stylesheet>

