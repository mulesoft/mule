<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:sq="http://www.webserviceX.NET/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/sq:string/sq:StockQuotes/sq:Stock">
        <org.mule.samples.stockquote.StockQuote>
            <name><xsl:value-of select="sq:Name"/></name>
            <symbol><xsl:value-of select="sq:Symbol"/></symbol>
            <date><xsl:value-of select="sq:Date"/><xsl:text> </xsl:text><xsl:value-of select="sq:Time"/></date>
            <change><xsl:value-of select="sq:Change"/></change>
            <last><xsl:value-of select="sq:Last"/></last>
            <open><xsl:value-of select="sq:Open"/></open>
            <high><xsl:value-of select="sq:High"/></high>
            <low><xsl:value-of select="sq:Low"/></low>
            <volume><xsl:value-of select="sq:Volume"/></volume>
            <previousClose><xsl:value-of select="sq:PreviousClose"/></previousClose>
        </org.mule.samples.stockquote.StockQuote>
    </xsl:template>
</xsl:stylesheet>