<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
        >

    <!-- $Id: -->

    <!-- Generates a table of Transport features. One entry per schema formatted correctly for Confluence.
         This can be used to list specific schema info
         which is useful for embedding on a transport reference page.
         The generated table will contain links to the schema doc and java doc, transport info such as meps and the maven
         artifact id.

         Usage:

         {xslt:style=^transport-feature-matrix.xsl|transport=SMTP|artifactName=email}
         <index version="3.0">
            <transport dist="ce" packageName="email">smtp</transport>
            <transport dist="ce" packageName="email">smtps</transport>
        </index>
        {xslt}

        For example purposes I have embedded the XML source, but really there should just be a single file that lists
        all transports
        If the 'transport' param is not set, a table will be created for all elements defined in the XML source.  This
        parameter should always be upper case
        The 'artifactName' is only needed when there is a module with more than one schema. For example email contains smtp, pop3, imap
         NOTE: this param can be removed once the schema-doc defines an artifact-name element
         -->

    <!-- We're rendering html markup -->
    <xsl:output method="html"/>

    <xsl:param name="transport"/>

    <xsl:param name="artifactName"/>
    <xsl:param name="version" select="/index/@version"/>

    <xsl:template match="/index">
        <table class="confluenceTable">
            <tbody>
                <tr>
                    <th class="confluenceTh">Transport</th>

                    <th class="confluenceTh">Doc</th>
                    <th class="confluenceTh">Inbound</th>
                    <th class="confluenceTh">Outbound</th>
                    <th class="confluenceTh">Request</th>
                    <th class="confluenceTh">Transactions</th>
                    <th class="confluenceTh">Streaming</th>
                    <th class="confluenceTh">Retries</th>
                    <th class="confluenceTh">MEPs</th>
                    <th class="confluenceTh">Default MEP</th>
                    <th class="confluenceTh">Maven Artifact</th>
                </tr>
                <xsl:apply-templates/>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template match="transport">
        <xsl:choose>
            <xsl:when test="@dist = 'ee'">
                <xsl:variable name="schemaLocation" select="concat('http://www.mulesoft.org/schema/mule/ee/',. , '/', $version, '/mule-', ., '-ee.xsd')"/>

                <xsl:apply-templates
                        select="document($schemaLocation)"/>
            </xsl:when>
            <xsl:when test="@dist = 'mf'">
                <xsl:variable name="schemaLocation">
                    <xsl:value-of select="@schema"/>
                </xsl:variable>
                <xsl:apply-templates select="document($schemaLocation)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="pname" select="@artifactName"/>

                <xsl:variable name="schemaLocation">
                    <xsl:choose>
                        <xsl:when test="@artifactName">

                            <xsl:value-of
                                    select="concat('http://svn.codehaus.org/mule/branches/mule-3.x/transports/', $pname ,'/src/main/resources/META-INF/mule-', ., '.xsd')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="concat('http://svn.codehaus.org/mule/branches/mule-3.x/transports/',. ,'/src/main/resources/META-INF/mule-', ., '.xsd')"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:apply-templates select="document($schemaLocation)"/>
                <!-- TODO use deployed schemas -->
                <!--<xsl:apply-templates select="document(concat('http://www.mulesoft.org/schema/mule/', .,'/', '$version', '/mule-', ., '.xsd'))"/>-->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="xsd:schema">
        <xsl:variable name="scheme" select="xsd:annotation/xsd:appinfo/schemadoc:short-name"/>

        <xsl:choose>
            <xsl:when test="starts-with($scheme, $transport)">
                <xsl:apply-templates select="xsd:annotation/xsd:appinfo"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="xsd:annotation/xsd:appinfo">
        <xsl:variable name="yes">
            <img class="emoticon" src="/documentation/images/icons/emoticons/check.gif" alt="" align="absmiddle"
                 border="0" height="16" width="16"/>
        </xsl:variable>

        <xsl:variable name="no">
            <img class="emoticon" src="/documentation/images/icons/emoticons/error.gif" alt="" align="absmiddle"
                 border="0" height="16" width="16"/>
        </xsl:variable>

        <!-- the lower-case function  not available in XSLT 1.0, using translate instead-->
        <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'"/>
        <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>

        <xsl:variable name="schemeUpper" select="schemadoc:short-name"/>

        <xsl:variable name="scheme" select="translate($schemeUpper, $uppercase, $lowercase)"/>

        <xsl:variable name="packName">
            <xsl:choose>
                <!-- set as a param (always when printing a single transport) -->
                <xsl:when test="$artifactName">
                    <xsl:value-of select="$artifactName"/>
                </xsl:when>
                <!-- set in the schema doc -->
                <xsl:when test="schemadoc:artifact-name">
                    <xsl:value-of select="schemadoc:artifact-name"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$scheme"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="receive">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@receiveEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="send">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@dispatchEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="request">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@requestEvents = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="transactions">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@transactions = 'true'">
                    <xsl:copy-of select="$yes"/>
                    <span>(<xsl:value-of select="schemadoc:transport-features/@transactionTypes"/>)
                    </span>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="stream">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@streaming = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="retry">
            <xsl:choose>
                <xsl:when test="schemadoc:transport-features/@retries = 'true'">
                    <xsl:copy-of select="$yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="meps">
            <xsl:value-of select="schemadoc:transport-features/schemadoc:MEPs/@supported"/>
        </xsl:variable>

        <xsl:variable name="defaultMep">
            <xsl:value-of select="schemadoc:transport-features/schemadoc:MEPs/@default"/>
        </xsl:variable>

        <xsl:variable name="page" select="schemadoc:page-title"/>

        <xsl:variable name="javaDoc">
            <a href="http://www.mulesoft.org/docs/site/current3/apidocs/org/mule/transport/{$packName}/package-summary.html">
                JavaDoc
            </a>
        </xsl:variable>
        <xsl:variable name="schemaDoc">
            <a href="http://www.mulesoft.org/docs/site/current3/schemadocs/namespaces/http_www_mulesoft_org_schema_mule_{$scheme}/namespace-overview.html">
                SchemaDoc
            </a>
        </xsl:variable>
        <xsl:variable name="maven">
            <xsl:value-of select="concat('org.mule.transport:mule-transport-', $scheme)"/>
        </xsl:variable>


        <tr>
            <td class="confluenceTd">
                <xsl:choose>
                    <xsl:when test="$transport">
                        <xsl:value-of select="$schemeUpper"/>
                    </xsl:when>
                    <!-- point to the artifact name as not to potentially create a dead link since not all schemas
                         will have a separate documentation page-->
                    <xsl:when test="$artifactName">
                        <a href="http://mule.mulesoft.org/display/MULE3USER/{$artifactName}+Transport+Reference">
                            <xsl:value-of select="$schemeUpper"/>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a href="http://mule.mulesoft.org/display/MULE3USER/{$schemeUpper}+Transport+Reference">
                            <xsl:value-of select="$schemeUpper"/>
                        </a>
                    </xsl:otherwise>
                </xsl:choose>

            </td>
            <td class="confluenceTd">
                <xsl:copy-of select="$javaDoc"/>
                <div> </div>
                <xsl:copy-of select="$schemaDoc"/>
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
                <xsl:copy-of select="$retry"/>
            </td>
            <td class="confluenceTd">
                <xsl:value-of select="$meps"/>
            </td>
            <td class="confluenceTd">
                <xsl:value-of select="$defaultMep"/>
            </td>
            <td class="confluenceTd">
                <xsl:value-of select="$maven"/>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
