<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
        >

    <!-- $Id: -->

    <!-- generate documentation for all elements

         to be embedded in confluence pages
    -->

    <!-- which elements should be displayed. Value can be one of:
      - all: render all elements (common first)
      - common: render connector, inbound-endpoint, outbound-endpoint and endpoint elements
      - specific: all other elements specific to the schema being processed
      - 'element name': The name of a single element to render
      -->
    <xsl:param name="show"/>

    <!-- We're rendering Wiki test -->
    <xsl:output method="text"/>

    <xsl:include href="http://www.mulesoft.org/xslt/mule/schemadoc/3.1/schemadoc-core-wiki.xsl"/>
    <!--Use this if testing locally -->
    <!--<xsl:include href="schemadoc-core-wiki.xsl"/>-->


    <xsl:template match="/">

        <xsl:variable name="display">
            <xsl:choose>
                <xsl:when test="$show">
                    <xsl:value-of select="$show"/>
                </xsl:when>
                <xsl:otherwise>all</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="$display = 'common' or $display = 'all'">
            <xsl:choose>
                <xsl:when test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:page-title">
                    h1. <xsl:value-of select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:page-title"/>
                </xsl:when>
                <xsl:otherwise>h1. Module (schemadoc:page-title not set)</xsl:otherwise>
            </xsl:choose>
            \\
            <xsl:value-of select="normalize-space(/xsd:schema/xsd:annotation/xsd:documentation)"/>

            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-common-elements']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-common-elements']"/>
            </xsl:if>

            <xsl:apply-templates select="/xsd:schema/xsd:element[@name='component']" mode="single-element"/>
            <xsl:apply-templates select="/xsd:schema/xsd:element[@name='transformer']" mode="single-element"/>
            <xsl:apply-templates select="/xsd:schema/xsd:element[@name='filter']" mode="single-element"/>

            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-common-elements']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-common-elements']"/>
            </xsl:if>
        </xsl:if>

        <!--<xsl:if test="$display = 'transformers' or $display = 'all'">-->
        <!--<xsl:if test="/xsd:schema/xsd:element[contains(@substitutionGroup,'abstract-transformer')]">-->
        <!--<xsl:call-template name="transformers"/>-->
        <!--</xsl:if>-->
        <!--</xsl:if>-->

        <!--<xsl:if test="$display = 'filters' or $display = 'all'">-->
        <!--<xsl:if test="/xsd:schema/xsd:element[contains(@substitutionGroup,'abstract-filter')]">-->
        <!--<xsl:call-template name="filters"/>-->
        <!--</xsl:if>-->
        <!--</xsl:if>-->

        <xsl:if test="$display = 'specific' or $display = 'all'">

            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-specific-elements']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-specific-elements']"/>
            </xsl:if>

            <xsl:apply-templates select="/xsd:schema/xsd:element[
                    @name!='transformer' and
                    @name!='component' and
                    @name!='filter']" mode="single-element"/>

            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-specific-elements']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-specific-elements']"/>
            </xsl:if>
        </xsl:if>

        <xsl:if test="$display != 'specific' and $display != 'all' and $display != 'common'">
            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-single-element']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='before-common-element']"/>
            </xsl:if>

            <xsl:apply-templates select="/xsd:schema/xsd:element[@name=$display]" mode="single-element"/>

            <xsl:if test="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-single-element']">
                <xsl:value-of
                        select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:additional-documentation[@where='after-single-element']"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
