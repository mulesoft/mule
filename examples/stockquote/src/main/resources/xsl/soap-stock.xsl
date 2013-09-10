<?xml version="1.0" encoding="ISO-8859-1"?>
<!--

    Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
    The software in this package is published under the terms of the CPAL v1.0
    license, a copy of which has been included with this distribution in the
    LICENSE.txt file.

-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/StockQuotes/Stock">
        <org.mule.example.stockquote.StockQuote>
            <name><xsl:value-of select="Name"/></name>
            <symbol><xsl:value-of select="Symbol"/></symbol>
            <date><xsl:value-of select="Date"/><xsl:text> </xsl:text><xsl:value-of select="Time"/></date>
            <change><xsl:value-of select="Change"/></change>
            <last><xsl:value-of select="Last"/></last>
            <open><xsl:value-of select="Open"/></open>
            <high><xsl:value-of select="High"/></high>
            <low><xsl:value-of select="Low"/></low>
            <volume><xsl:value-of select="Volume"/></volume>
            <previousClose><xsl:value-of select="PreviousClose"/></previousClose>
        </org.mule.example.stockquote.StockQuote>
    </xsl:template>
</xsl:stylesheet>
