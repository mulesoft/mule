<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        exclude-result-prefixes="xsd"
        >

    <!-- $Id: -->

    <!-- generate text to cut+paste into the wiki and links document

         this should be run on a transport's schema.

         for example,
         saxon ./transports/http/src/main/resources/META-INF/mule-http.xsd \
               ./tools/schemadocs/src/main/resources/transport-to-links.xsl transport=http
    -->

    <!-- the transport we are generating docs for -->
    <xsl:param name="transport"/>

    <!-- transport or module -->
    <xsl:param name="type" select="'Transport'"/>

    <xsl:output method="xml"/>

    <xsl:template match="/">
        <xsl:apply-templates select="//xsd:element" mode="links"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="xsd:element[@name and not(@abstract='true')]" mode="links">
        <xsl:text>
</xsl:text><link><item><xsl:value-of select="$transport"/>:<xsl:value-of select="@name"/></item><page><xsl:value-of select="upper-case(substring($transport, 1, 1))"/><xsl:value-of select="substring($transport, 2)"/>+<xsl:value-of select="$type"/></page></link>
    </xsl:template>

    <!-- discard unnamed elements to avoid default text() copying -->

    <xsl:template match="xsd:element" mode="links"/>

</xsl:stylesheet>
