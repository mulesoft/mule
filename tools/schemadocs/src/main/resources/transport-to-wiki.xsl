<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <!-- $Id: -->

    <!-- generate text to cut+paste into the wiki and links document

         this should be run on a transport's schema

         for example,
         saxon ./transports/http/src/main/resources/META-INF/mule-https.xsd \
               ./tools/schemadocs/src/main/resources/transport-to-wiki.xsl transport=https

    -->

    <!-- the transport we are generating docs for -->
    <xsl:param name="transport"/>

    <xsl:output method="text"/>

    <xsl:template match="/">
h2. Detailed Configuration Information
        <xsl:apply-templates select="//xsd:element[@name='connector']" mode="wiki-menu-connector"/>
        <xsl:apply-templates select="//xsd:element[@name='inbound-endpoint']" mode="wiki-menu"/>
        <xsl:apply-templates select="//xsd:element[@name='outbound-endpoint']" mode="wiki-menu"/>
        <xsl:apply-templates select="//xsd:element[@name='endpoint']" mode="wiki-menu-global"/>
        <xsl:apply-templates select="//xsd:element[@name!='connector'and@name!='endpoint'and@name!='inbound-endpoint'and@name!='outbound-endpoint']" mode="wiki-menu"/>

        <xsl:apply-templates select="//xsd:element[@name='connector']" mode="wiki-content"/>
        <xsl:apply-templates select="//xsd:element[@name='inbound-endpoint']" mode="wiki-content"/>
        <xsl:apply-templates select="//xsd:element[@name='outbound-endpoint']" mode="wiki-content"/>
        <xsl:apply-templates select="//xsd:element[@name='endpoint']" mode="wiki-content"/>
        <xsl:apply-templates select="//xsd:element[@name!='connector'and@name!='endpoint'and@name!='inbound-endpoint'and@name!='outbound-endpoint']" mode="wiki-content"/>
        <xsl:text>

</xsl:text>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-menu-connector"><xsl:variable name="textname" select="translate(@name, '-', ' ')"/>
* [<xsl:value-of select="upper-case(substring($transport, 1, 1))"/><xsl:value-of select="substring($transport, 2)"/> connector|#<xsl:value-of select="$transport"/>-<xsl:value-of select="@name"/>]</xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-menu-global"><xsl:variable name="textname" select="translate(@name, '-', ' ')"/>
* [Global endpoint|#<xsl:value-of select="$transport"/>-<xsl:value-of select="@name"/>]</xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-menu"><xsl:variable name="textname" select="translate(@name, '-', ' ')"/>
* [<xsl:value-of select="upper-case(substring($textname, 1, 1))"/><xsl:value-of select="substring($textname, 2)"/>|#<xsl:value-of select="$transport"/>-<xsl:value-of select="@name"/>]</xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-content">

{cache:showDate=true|showRefresh=true}
{xslt:style=#http://svn.codehaus.org/mule/branches/mule-2.0.x/tools/schemadocs/src/main/resources/xslt/single-element.xsl|source=#http://dev.mulesoft.com/docs/xsd-doc/normalized.xsd|elementName=<xsl:value-of select="$transport"/>:<xsl:value-of select="@name"/>}
{xslt}
{cache}

\\</xsl:template>

</xsl:stylesheet>
