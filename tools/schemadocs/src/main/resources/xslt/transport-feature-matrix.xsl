<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
        >

    <!-- $Id: -->

    <!-- Generates a table of Transport features. One entry per schema -->

    <!-- Should a table header be generated -->
    <xsl:param name="header"/>

    <!-- We're rendering wiki markup -->
    <xsl:output method="text"/>

    <xsl:template match="/">
        <xsl:apply-templates select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:transport-features"/>
    </xsl:template>

    <xsl:template match="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:transport-features">

        <xsl:variable name="yes">
            <img class="emoticon" src="/images/icons/emoticons/check.gif" height="16" width="16" align="absmiddle"
                 alt="" border="0"/>
        </xsl:variable>

        <xsl:variable name="no">
            <img class="emoticon" src="/images/icons/emoticons/error.gif" height="16" width="16" align="absmiddle"
                 alt="" border="0"/>
        </xsl:variable>

        <xsl:variable name="page" select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:page-title"/>

        <xsl:variable name="receive">
            <xsl:choose>
                <xsl:when test="@receiveEvents = 'true'">
                    <xsl:value-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="send">
            <xsl:choose>
                <xsl:when test="@dispatchEvents = 'true'">
                    <xsl:value-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="request">
            <xsl:choose>
                <xsl:when test="@requestEvents = 'true'">
                    <xsl:value-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="trans">
            <xsl:choose>
                <xsl:when test="@transactions = 'true'">
                    <xsl:value-of select="$yes"/>
                    <xsl:value-of select="@transaction-types"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="stream">
            <xsl:choose>
                <xsl:when test="@streaming = 'true'">
                    <xsl:value-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="meps">
            <xsl:value-of select="schemadoc:MEPs/@supported"/>
        </xsl:variable>

        <xsl:variable name="defaultMep">
            <xsl:value-of select="schemadoc:MEPs/@default"/>
        </xsl:variable>


        <xsl:if test="$header = 'true'">
||Transport||Receive Events||Dispatch Events||Request Events||Transactions||Streaming||MEPs||Default MEP||
        </xsl:if>

|[<xsl:value-of select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:short-name"/>|http://mule.mulesoft.org/display/MULE2USER/{$page}]|<xsl:apply-templates select="@*"/>|<xsl:value-of select="$meps"/>|<xsl:value-of select="$defaultMep"/>|
    </xsl:template>

    <xsl:template match="@*">
|<xsl:choose><xsl:when test=". = 'true'">(/)</xsl:when><xsl:otherwise>(x)</xsl:otherwise></xsl:choose>|
    </xsl:template>
</xsl:stylesheet>
