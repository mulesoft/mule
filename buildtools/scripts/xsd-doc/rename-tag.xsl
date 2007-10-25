<xsl:stylesheet
   version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
   >

    <!-- $Id$ -->

    <!-- this renames element, group and type names so that we can process several
         schema together.  see further comments in normalize.sh -->

    <xsl:output method="xml" omit-xml-declaration="yes"/>
    <xsl:param name="tag"/>

    <xsl:template match="/">
        <xsl:apply-templates select="./*"/>
    </xsl:template>

    <xsl:template match="xsd:element">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:attribute">
        <xsl:copy>
            <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
            <xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
            <xsl:apply-templates select="./*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xsd:complexType">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:complexContent">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:extension">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:group">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:sequence">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:choice">
        <xsl:call-template name="copyWithRename"/>
    </xsl:template>

    <xsl:template match="xsd:annotation">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template name="copyWithRename">
        <xsl:copy>
            <xsl:if test="@name">
                <xsl:choose>
                    <xsl:when test="contains(@name, ':')">
                        <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="name"><xsl:value-of select="concat($tag, ':', @name)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="@ref">
                <xsl:choose>
                    <xsl:when test="contains(@ref, ':')">
                        <xsl:attribute name="ref"><xsl:value-of select="@ref"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="ref"><xsl:value-of select="concat($tag, ':', @ref)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="@base">
                <xsl:choose>
                    <xsl:when test="contains(@base, ':')">
                        <xsl:attribute name="base"><xsl:value-of select="@base"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="base"><xsl:value-of select="concat($tag, ':', @base)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="@type">
                <xsl:choose>
                    <xsl:when test="contains(@type, ':')">
                        <xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="type"><xsl:value-of select="concat($tag, ':', @type)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="@substitutionGroup">
                <xsl:choose>
                    <xsl:when test="contains(@substitutionGroup, ':')">
                        <xsl:attribute name="substitutionGroup"><xsl:value-of select="@substitutionGroup"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="substitutionGroup"><xsl:value-of select="concat($tag, ':', @substitutionGroup)"/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <xsl:apply-templates select="./*"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>