<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/StockQuotes/Stock">
        <org.mule.samples.stockquote.StockQuote>
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
        </org.mule.samples.stockquote.StockQuote>
    </xsl:template>
</xsl:stylesheet>