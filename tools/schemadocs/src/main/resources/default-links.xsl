<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <!-- $Id$ -->

    <!-- generate an initial links file that includes standard locations for basic elements

         this should be run on the normalized schema
    -->

    <xsl:output method="xml"/>

    <xsl:template match="/">
        <links>
            <xsl:call-template name="newline"/>
            <xsl:apply-templates select="//xsd:element[@name]"/>
            <xsl:call-template name="newline"/>
        </links>
    </xsl:template>

    <xsl:template match="xsd:element[@name]">
        <link>
            <xsl:call-template name="newline"/>
            <item>
                <xsl:value-of select="@name"/>
            </item>
            <page>
                <xsl:choose>
                    <xsl:when test="starts-with(@name, 'mule:')">SchemaBasics</xsl:when>
                    <!-- more logic here? -->
                </xsl:choose>
            </page>
            <xsl:call-template name="newline"/>
        </link>
    </xsl:template>

    <xsl:template name="newline">
        <xsl:text>
</xsl:text>
    </xsl:template>

</xsl:stylesheet>
