<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
        >

    <!-- $Id: -->

    <!-- Generates a table of Transport features. One entry per schema -->

    <!-- We're rendering html markup -->
    <xsl:output method="html"/>

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="/index">
        <h2>Transport Feature Matrix (Mule 3.0)</h2>
        <table class="confluenceTable">
            <tbody>
                <tr>
                    <th class="confluenceTh">Transport</th>
                    <th class="confluenceTh">Receive Events</th>
                    <th class="confluenceTh">Dispatch Events</th>
                    <th class="confluenceTh">Request Events</th>
                    <th class="confluenceTh">Transactions</th>
                    <th class="confluenceTh">Streaming</th>
                    <th class="confluenceTh">MEPs</th>
                    <th class="confluenceTh">Default MEP</th>
                </tr>
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>
    <xsl:template match="transport">
        <xsl:variable name="version"><xsl:value-of select="/index/@version"/></xsl:variable>
        <xsl:choose>
            <xsl:when test="@dist = 'ee'">
                <xsl:apply-templates select="document(concat('http://www.mulesource.org/schema/mule/ee/',. ,'$version', '/mule-', ., '-ee.xsd'))"/>
            </xsl:when>
            <xsl:when test="@dist = 'mf'">
                <xsl:variable name="schemaLocation"><xsl:value-of select="@schema"/></xsl:variable>
                <xsl:apply-templates select="document($schemaLocation)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="schemaLocation">
                <xsl:choose>
                    <xsl:when test="@schema">
                        <xsl:value-of select="@schema"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat('http://svn.codehaus.org/mule/branches/mule-3.x/transports/',. ,'/src/main/resources/META-INF/mule-', ., '.xsd')"/>
                    </xsl:otherwise>
                </xsl:choose>
                </xsl:variable>
                <xsl:apply-templates select="document($schemaLocation)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="xsd:schema">
        <xsl:apply-templates select="xsd:annotation/xsd:appinfo/schemadoc:transport-features"/>
    </xsl:template>

    <xsl:template match="xsd:annotation/xsd:appinfo/schemadoc:transport-features">
        <xsl:variable name="yes">
            <img class="emoticon" src="/documentation/images/icons/emoticons/check.gif" alt="" align="absmiddle" border="0" height="16" width="16"/>
        </xsl:variable>

        <xsl:variable name="no">
            <img class="emoticon" src="/documentation/images/icons/emoticons/error.gif" alt="" align="absmiddle" border="0" height="16" width="16"/>
        </xsl:variable>


        <xsl:variable name="receive">
            <xsl:choose>
                <xsl:when test="@receiveEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="send">
            <xsl:choose>
                <xsl:when test="@dispatchEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="request">
            <xsl:choose>
                <xsl:when test="@requestEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="transactions">
            <xsl:choose>
                <xsl:when test="@transactions = 'true'">
                    <xsl:copy-of select="$yes"/><span> (<xsl:value-of select="@transactionTypes"/>)</span>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="stream">
            <xsl:choose>
                <xsl:when test="@streaming = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="meps">
            <xsl:value-of select="schemadoc:MEPs/@supported"/>
        </xsl:variable>

        <xsl:variable name="defaultMep">
            <xsl:value-of select="schemadoc:MEPs/@default"/>
        </xsl:variable>
        <xsl:variable name="page">
            <xsl:value-of select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:page-title"/>
        </xsl:variable>
        <tr>
            <td class="confluenceTd"><a href="concat('http://mule.mulesoft.org/display/MULE2USER/', translate($page, ' ', '+'))"><xsl:value-of
                    select="/xsd:schema/xsd:annotation/xsd:appinfo/schemadoc:short-name"/></a>
            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$receive"/>
            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$send"/>
            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$request"/>
            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$transactions"/>
            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$stream"/>
            </td>
            <td class="confluenceTd">
                <xsl:value-of select="$meps"/>
            </td>
            <td class="confluenceTd">
                <xsl:value-of select="$defaultMep"/>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
