<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <!-- $Id: -->

    <!-- generate documentation for an entire transport or module

         to be embedded in confluence pages
    -->

    <!-- the transport we are generating docs for -->
    <!--
    <xsl:param name="transport"/>
    <xsl:variable name="prefix" select="concat($transport, ':')"/>
    <xsl:variable name="abstract" select="concat($prefix, 'abstract')"/>
   -->
    <xsl:output method="html"/>
    <!-- xsl:include href="schemadoc-core.xsl"/ -->
    <xsl:include
            href="http://svn.codehaus.org/mule/branches/mule-2.0.x/tools/schemadocs/src/main/resources/xslt/schemadoc-core.xsl"/>

    <xsl:template match="/">
        <html>
            <body>
                <h2>Detailed Configuration Information</h2>
                <ul>
                    <xsl:apply-templates select="//xsd:element[@name='connector']" mode="wiki-menu"/>
                    <xsl:apply-templates select="//xsd:element[@name='inbound-endpoint']" mode="wiki-menu"/>
                    <xsl:apply-templates select="//xsd:element[@name='outbound-endpoint']" mode="wiki-menu"/>
                    <xsl:apply-templates select="//xsd:element[@name='endpoint']" mode="wiki-menu"/>
                    <xsl:apply-templates select="//xsd:element[
        @name!='connector' and
        @name!='endpoint' and
        @name!='inbound-endpoint' and
        @name!='outbound-endpoint' and
        starts-with(@name, $prefix) and
        not(starts-with(@name, $abstract))]" mode="wiki-menu"/>
                </ul>

                <xsl:apply-templates select="//xsd:element[@name='connector']" mode="single-element"/>
                <xsl:apply-templates select="//xsd:element[@name='inbound-endpoint']" mode="single-element"/>
                <xsl:apply-templates select="//xsd:element[@name='outbound-endpoint']" mode="single-element"/>
                <xsl:apply-templates select="//xsd:element[@name='endpoint']" mode="single-element"/>
                <xsl:apply-templates select="//xsd:element[
        @name!=concat($prefix, 'connector') and
        @name!=concat($prefix, 'endpoint') and
        @name!=concat($prefix, 'inbound-endpoint') and
        @name!=concat($prefix, 'outbound-endpoint') and
        not(starts-with(@name, 'abstract'))]" mode="single-element"/>
        <xsl:text>

</xsl:text>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="wiki-menu">
        <li>
            <xsl:call-template name="link">
                <xsl:with-param name="item">
                    <xsl:value-of select="@name"/>
                </xsl:with-param>
            </xsl:call-template>
        </li>
    </xsl:template>

</xsl:stylesheet>
