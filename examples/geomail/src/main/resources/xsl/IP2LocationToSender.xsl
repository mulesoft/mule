<?xml version='1.0'?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:fraudlabs="http://ws.fraudlabs.com/" exclude-result-prefixes="fraudlabs xsi">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <xsl:apply-templates select="/fraudlabs:IP2LOCATION"/>
    </xsl:template>

    <xsl:template match="fraudlabs:IP2LOCATION">
        <sender xmlns="http://www.mulesource.org/example/geomail">
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