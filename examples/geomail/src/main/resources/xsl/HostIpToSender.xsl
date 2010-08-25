<?xml version='1.0'?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:hostip="http://www.hostip.info/api"
                xmlns:gml="http://www.opengis.net/gml" exclude-result-prefixes="gml hostip xsi">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <sender xmlns="http://www.mulesoft.org/example/geomail">
            <xsl:apply-templates/>
        </sender>
    </xsl:template>

    <xsl:template match="/HostipLookupResultSet">
        <xsl:apply-templates select="gml:featureMember"/>
    </xsl:template>

    <xsl:template match="gml:featureMember">
        <xsl:apply-templates select="Hostip"/>
    </xsl:template>

    <xsl:template match="Hostip">
        <locationName>
            <xsl:value-of select="gml:name"/>
        </locationName>
        <countryName>
            <xsl:value-of select="countryName"/>
        </countryName>
        <ip>
            <xsl:value-of select="ip"/>
        </ip>
        <xsl:apply-templates select="ipLocation"/>
    </xsl:template>

    <xsl:template match="ipLocation">
        <xsl:variable name="pos" select="gml:pointProperty/gml:Point/gml:coordinates"/>
        <xsl:variable name="lon" select="substring-before($pos, ',')"/>
        <xsl:variable name="lat" select="substring-after($pos, ',')"/>
        <latitude>
            <xsl:value-of select="$lat"/>
        </latitude>
        <longitude>
            <xsl:value-of select="$lon"/>
        </longitude>
    </xsl:template>
</xsl:stylesheet>
