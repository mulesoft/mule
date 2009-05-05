<?xml version='1.0'?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:hostip="http://www.hostip.info/api"
                xmlns:gml="http://www.opengis.net/gml" exclude-result-prefixes="gml hostip xsi">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <sender xmlns="http://www.mulesource.org/example/geomail">
            <xsl:apply-templates/>
        </sender>
    </xsl:template>

    <xsl:template match="hostip:HostipLookupResultSet">
        <xsl:apply-templates select="gml:featureMember"/>
    </xsl:template>

    <xsl:template match="gml:featureMember">
        <xsl:apply-templates select="hostip:Hostip"/>
    </xsl:template>

    <xsl:template match="hostip:Hostip">
        <locationName>
            <xsl:value-of select="gml:name"/>
        </locationName>
        <countryName>
            <xsl:value-of select="hostip:countryName"/>
        </countryName>
        <xsl:apply-templates select="hostip:ipLocation"/>
    </xsl:template>

    <xsl:template match="hostip:ipLocation">
        <xsl:variable name="pos" select="gml:PointProperty/gml:Point/gml:coordinates"/>
        <xsl:variable name="lon" select="substring-before($pos, ',')"/>
        <xsl:variable name="lat" select="substring-after($pos, ',')"/>
        <ip>
            <xsl:value-of select="@ip"/>
        </ip>
        <latitude>
            <xsl:value-of select="$lat"/>
        </latitude>
        <longitude>
            <xsl:value-of select="$lon"/>
        </longitude>
    </xsl:template>


</xsl:stylesheet>