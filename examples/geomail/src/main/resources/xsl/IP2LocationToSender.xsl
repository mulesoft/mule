<?xml version='1.0'?>
<!--

    Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
    The software in this package is published under the terms of the CPAL v1.0
    license, a copy of which has been included with this distribution in the
    LICENSE.txt file.

-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:fraudlabs="http://ws.fraudlabs.com/" exclude-result-prefixes="fraudlabs xsi">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <xsl:apply-templates select="/fraudlabs:IP2LOCATION"/>
    </xsl:template>

    <xsl:template match="fraudlabs:IP2LOCATION">
        <sender xmlns="http://www.mulesoft.org/example/geomail">
            <locationName><xsl:value-of select="fraudlabs:CITY"/>,
                <xsl:value-of select="fraudlabs:REGION"/>
            </locationName>
            <countryName>
                <xsl:value-of select="fraudlabs:COUNTRYNAME"/>
            </countryName>
            <latitude>
                <xsl:value-of select="fraudlabs:LATITUDE"/>
            </latitude>
            <longitude>
                <xsl:value-of select="fraudlabs:LONGITUDE"/>
            </longitude>
        </sender>

    </xsl:template>

</xsl:stylesheet>
