<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesource.org/schema/mule/schemadoc/2.2"
        >

    <!-- $Id: -->

    <!-- Generates a table of Transport features. One entry per schema -->

    <!-- Should a table header be generated -->
    <xsl:param name="header"/>

    <!-- We're rendering Html -->
    <xsl:output method="html"/>

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
        <xsl:variable name="response">
            <xsl:choose>
                <xsl:when test="@responseEvents = 'true'">
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
        <xsl:variable name="inmeps">
            <xsl:choose>
                <xsl:when test="schemadoc:inboundMEPs/@none = 'true'">None</xsl:when>
                <xsl:otherwise>
                    <xsl:if test="schemadoc:inboundMEPs/@in-only = 'true'">In-Only </xsl:if>
                    <xsl:if test="schemadoc:inboundMEPs/@in-out = 'true'">In-Out </xsl:if>
                    <xsl:if test="schemadoc:inboundMEPs/@in-optional-out = 'true'">In-Optional-Out </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="outmeps">
            <xsl:choose>
                <xsl:when test="schemadoc:outboundMEPs/@none = 'true'">None</xsl:when>
                <xsl:otherwise>
                    <xsl:if test="schemadoc:outboundMEPs/@out-only = 'true'">Out-Only </xsl:if>
                    <xsl:if test="schemadoc:outboundMEPs/@out-in = 'true'">Out-In </xsl:if>
                    <xsl:if test="schemadoc:outboundMEPs/@out-optional-in = 'true'">Out-Optional-In </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>


        <xsl:if test="$header = 'true'">
            <tr>
                <th class="confluenceTh" style="width:10%">Transport</th>
                <th class="confluenceTh" style="width:10%">Receive Events</th>
                <th class="confluenceTh" style="width:10%">Dispatch Events</th>
                <th class="confluenceTh" style="width:10%">Request Events</th>
                <th class="confluenceTh" style="width:10%">Request/Response Events</th>
                <th class="confluenceTh" style="width:10%">Transactions</th>
                <th class="confluenceTh" style="width:10%">Streaming</th>
                <th class="confluenceTh" style="width:10%">Inbound MEPs</th>
                <th class="confluenceTh" style="width:10%">Outbound MEPs</th>
            </tr>
        </xsl:if>

        <tr>
            <td class="confluenceTd" rowspan="1"><a href="http://mule.mulesource.org/display/MULE2USER/{$page}"><xsl:value-of
                select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:short-name"/></a></td>
            <xsl:apply-templates select="@*"/>
            <td class="confluenceTd" rowspan="1">
                <xsl:value-of select="$inmeps"/>
            </td>
            <td class="confluenceTd" rowspan="1">
                <xsl:value-of select="$outmeps"/>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="@*">
        <td class="confluenceTd" rowspan="1">
                <xsl:choose>
                    <xsl:when test=". = 'true'">
                        <img class="emoticon" src="/images/icons/emoticons/check.gif" height="16" width="16"
                             align="absmiddle" alt="" border="0"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <img class="emoticon" src="/images/icons/emoticons/error.gif" height="16" width="16"
                             align="absmiddle" alt="" border="0"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
    </xsl:template>
</xsl:stylesheet>
