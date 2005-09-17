<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:amazon="http://webservices.amazon.com/AWSECommerceService/2005-07-26" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/amazon:ItemSearchResponse/amazon:Items">
        Total Results:<xsl:value-of select="amazon:TotalResults"/>
        <xsl:for-each select="amazon:Item">
            <xsl:value-of select="amazon:ItemAttributes/amazon:Title"/>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>