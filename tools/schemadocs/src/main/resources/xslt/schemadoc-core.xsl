<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <!-- the table of pages for linking -->
    <xsl:key name="item-to-page" match="link" use="item"/>
    <xsl:variable name="items-to-pages" select="document('http://svn.codehaus.org/mule/branches/mule-3.1.x/tools/schemadocs/src/main/resources/links.xml')/links"/>

    <xsl:template match="xsd:element" mode="single-element">
        <a>
            <!-- define a tag we can link to -->
            <xsl:attribute name="id">
                <xsl:call-template name="anchor">
                    <xsl:with-param name="item">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:attribute>
            <h2>&lt;<xsl:value-of select="@name"/> ...&gt;</h2>
        </a>
        <!-- p>
            <em>This documentation is automatically generated from the XML schema.
            We are still extending the documentation and improving the presentation;
            please bear with us.
            To add similar documentation to other pages, examine the source of this
            page and copy the {cache ....} section that contains {xslt ...}.
            Change the "elementName" parameter to select the element you want displayed.</em>
        </p -->
        <xsl:apply-templates select="." mode="documentation"/>
        <h3>Attributes</h3>
        <table class="confluenceTable">
            <th class="confluenceTh" style="width:10%">Name</th>
            <th class="confluenceTh" style="width:10%">Type</th>
            <th class="confluenceTh" style="width:10%">Required</th>
            <th class="confluenceTh" style="width:10%">Default</th>
            <th class="confluenceTh">Description</th>
            <xsl:apply-templates select="." mode="attributes"/>
        </table>

        <h3>Child Elements</h3>
        <table class="confluenceTable">
            <tr>
                <th class="confluenceTh" style="width:20%">Name</th>
                <th class="confluenceTh">Cardinality</th>
                <th class="confluenceTh">Description</th>
            </tr>
            <xsl:call-template name="element-children"/>
            <xsl:if test="@type">
                <xsl:variable name="type" select="@type"/>
                <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$type]" mode="elements"/>
            </xsl:if>
        </table>
    </xsl:template>


    <!-- documentation

       we need to collect documentation from:
       - the ref
       - the element
       - the type
       we don't use extension or substitution here -->

    <xsl:template match="xsd:element[@ref]" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <p>
                <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()" mode="copy"/>
                <xsl:call-template name="attribution">
                    <xsl:with-param name="text">
                        From reference for element <xsl:value-of select="@ref"/>.
                    </xsl:with-param>
                </xsl:call-template>
            </p>
        </xsl:if>
        <xsl:variable name="ref" select="@ref"/>
        <xsl:apply-templates
                select="/xsd:schema/xsd:element[@name=$ref]" mode="documentation"/>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <p>
                <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()" mode="copy"/>
                <xsl:call-template name="attribution">
                    <xsl:with-param name="text">
                        From declaration of element <xsl:value-of select="@name"/>.
                    </xsl:with-param>
                </xsl:call-template>
            </p>
        </xsl:if>
        <xsl:if test="@type">
            <xsl:variable name="type" select="@type"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:complexType[@name=$type]" mode="documentation"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="@*|node()" mode="copy">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="copy"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <p>
                <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()" mode="copy"/>
                <xsl:call-template name="attribution">
                    <xsl:with-param name="text">
                        <xsl:choose>
                            <xsl:when test="@name">
                                From declaration of type <xsl:value-of select="@name"/>.
                            </xsl:when>
                            <xsl:otherwise>
                                From type declaration.
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
            </p>
        </xsl:if>
    </xsl:template>


    <!-- attributes -->

    <xsl:template match="xsd:attribute[@name]" mode="attributes">
        <tr>
            <td class="confluenceTd" rowspan="1">
                <xsl:value-of select="@name"/>
            </td>
            <td class="confluenceTd" style="text-align: center">
                <xsl:choose>
                    <xsl:when test="string-length(@type)">
                        <xsl:call-template name="rewrite-type">
                            <xsl:with-param name="type" select="@type"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="xsd:simpleType/xsd:restriction/xsd:enumeration">
                        <xsl:for-each select="xsd:simpleType/xsd:restriction/xsd:enumeration">
                            <xsl:if test="@value">
                                <b><xsl:value-of select="@value"/></b>
                                <xsl:if test="position()!=last()"> / </xsl:if>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                </xsl:choose>
            </td>
            <td class="confluenceTd" style="text-align: center">
                <xsl:choose>
                    <xsl:when test="@required">yes</xsl:when>
                    <xsl:otherwise>no</xsl:otherwise>
                </xsl:choose>
            </td>
            <td class="confluenceTd" style="text-align: center">
                <xsl:if test="@default"><xsl:value-of select="@default"/></xsl:if>
            </td>
            <td class="confluenceTd">
                <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
                    <p>
                        <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()" mode="copy"/>
                    </p>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="xsd:element" mode="attributes">
        <xsl:call-template name="attribute-children"/>
        <xsl:if test="@ref">
            <xsl:variable name="ref" select="@ref"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:element[@name=$ref]" mode="attributes"/>
        </xsl:if>
        <xsl:if test="@type">
            <xsl:variable name="type" select="@type"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:complexType[@name=$type]" mode="attributes"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="attribute-children">
        <xsl:apply-templates select="xsd:attribute" mode="attributes"/>
        <xsl:apply-templates select="xsd:attributeGroup" mode="attributes"/>
        <xsl:apply-templates select="xsd:sequence" mode="attributes"/>
        <xsl:apply-templates select="xsd:choice" mode="attributes"/>
        <xsl:apply-templates select="xsd:complexType" mode="attributes"/>
        <xsl:apply-templates select="xsd:complexContent" mode="attributes"/>
        <xsl:apply-templates select="xsd:extension" mode="attributes"/>
    </xsl:template>

    <xsl:template match="xsd:attributeGroup" mode="attributes">
        <xsl:if test="@ref">
            <xsl:variable name="ref" select="@ref"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:attributeGroup[@name=$ref]" mode="attributes"/>
        </xsl:if>
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

    <xsl:template match="xsd:sequence" mode="attributes">
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

    <xsl:template match="xsd:choice" mode="attributes">
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="attributes">
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

    <xsl:template match="xsd:complexContent" mode="attributes">
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

    <xsl:template match="xsd:extension" mode="attributes">
        <xsl:variable name="base" select="@base"/>
        <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$base]" mode="attributes"/>
        <xsl:call-template name="attribute-children"/>
    </xsl:template>


    <!-- child elements -->
    <!-- documentation here more restricted than "documentation" mode -->

    <xsl:template match="xsd:element[@ref]" mode="elements">
        <!--element ref <xsl:value-of select="@ref"/>-->
        <tr>
            <xsl:variable name="ref" select="@ref"/>
            <td class="confluenceTd" rowspan="1">
                <xsl:choose>
                    <xsl:when test="contains(@ref, ':abstract-')">
                        <xsl:variable name="name" select="substring-after(@ref, ':abstract-')"/>
                        A <xsl:value-of select="$name"/> element
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="link">
                            <xsl:with-param name="item">
                                <xsl:value-of select="@ref"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- cardinality i.e. minoccurs/maxoccurs -->
            <xsl:variable name="min">
                <xsl:choose>
                    <xsl:when test="@minOccurs"><xsl:value-of select="@minOccurs"/></xsl:when>
                    <xsl:otherwise><xsl:text>1</xsl:text></xsl:otherwise>
                </xsl:choose>
           </xsl:variable>
            <xsl:variable name="max">
                <xsl:choose>
                    <xsl:when test="@maxOccurs='unbounded'"><xsl:text>*</xsl:text></xsl:when>
                    <xsl:when test="@maxOccurs"><xsl:value-of select="@maxOccurs"/></xsl:when>
                    <xsl:otherwise><xsl:text>1</xsl:text></xsl:otherwise>
                </xsl:choose>
           </xsl:variable>
            <td class="confluenceTd">
                <xsl:value-of select="$min"/>..<xsl:value-of select="$max"/>
            </td>
            <td class="confluenceTd">
                <!-- include both ref and element doc -->
                <xsl:apply-templates select="." mode="elements-doc"/>
                <xsl:apply-templates
                        select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-doc"/>
                <xsl:apply-templates
                        select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-abstract"/>
            </td>
        </tr>
        <!--element ref done-->
    </xsl:template>

    <xsl:template match="xsd:element[contains(@name, ':abstract-')]"
                  mode="elements-abstract">
        <!--element (abstract) <xsl:value-of select="@name"/>-->
        <xsl:variable name="name" select="@name"/>
        <p>
            <xsl:choose>
                <!-- this should always be true when using the normalized schema -->
                <xsl:when test="/xsd:schema/xsd:element[@substitutionGroup=$name]">
                    The following elements can be used here:
                    <ul>
                        <xsl:apply-templates
                                select="/xsd:schema/xsd:element[@substitutionGroup=$name]"
                                mode="elements-list"/>
                    </ul>
                </xsl:when>
                <xsl:otherwise>
                    This is an abstract element; another element with a compatible
                    type must be used in its place.  However, no replacements were
                    found when generating this documentation.
                </xsl:otherwise>
            </xsl:choose>
        </p>
        <!--element (abstract) done-->
    </xsl:template>

    <!-- otherwise, do nothing -->
    <xsl:template match="xsd:element" mode="elements-abstract"/>

    <xsl:template match="xsd:element[@name]" mode="elements-list">
        <!--element (list) <xsl:value-of select="@name"/>-->
        <li>
            <xsl:call-template name="link">
                <xsl:with-param name="item">
                    <xsl:value-of select="@name"/>
                </xsl:with-param>
            </xsl:call-template>
        </li>
        <!-- li>&lt;<xsl:value-of select="@name"/> ...&gt;</li -->
        <!--element (list) done-->
    </xsl:template>

    <xsl:template match="xsd:element" mode="elements-doc">
        <!--element (doc) <xsl:value-of select="@name"/>-->
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <p>
                <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()" mode="copy"/>
            </p>
        </xsl:if>
        <!--element (doc) done-->
    </xsl:template>

    <xsl:template name="element-children">
        <!--children-->
        <xsl:apply-templates select="xsd:element" mode="elements"/>
        <xsl:apply-templates select="xsd:group" mode="elements"/>
        <xsl:apply-templates select="xsd:sequence" mode="elements"/>
        <xsl:apply-templates select="xsd:choice" mode="elements"/>
        <xsl:apply-templates select="xsd:complexType" mode="elements"/>
        <xsl:apply-templates select="xsd:complexContent" mode="elements"/>
        <xsl:apply-templates select="xsd:extension" mode="elements"/>
        <!--children done-->
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="elements">
        <!--element <xsl:value-of select="@name"/>-->
        <tr>
            <xsl:variable name="name" select="@name"/>
            <td class="confluenceTd" rowspan="1">
                <xsl:call-template name="link">
                    <xsl:with-param name="item">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                </xsl:call-template>
            </td>
            <!-- cardinality i.e. minoccurs/maxoccurs -->
            <xsl:variable name="min">
                <xsl:choose>
                    <xsl:when test="@minOccurs"><xsl:value-of select="@minOccurs"/></xsl:when>
                    <xsl:otherwise>1</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="max">
                <xsl:choose>
                    <xsl:when test="@maxOccurs='unbounded'">*</xsl:when>
                    <xsl:when test="@maxOccurs"><xsl:value-of select="@maxOccurs"/></xsl:when>
                    <xsl:otherwise>1</xsl:otherwise>
                </xsl:choose>
           </xsl:variable>
            <td class="confluenceTd">
                <xsl:value-of select="$min"/>..<xsl:value-of select="$max"/>
            </td>

            <td class="confluenceTd">
                <xsl:apply-templates select="." mode="elements-doc"/>
            </td>
        </tr>
        <!--element done-->
    </xsl:template>

    <xsl:template match="xsd:group" mode="elements">
        <!--group <xsl:value-of select="@name"/>-->
        <xsl:if test="@ref">
            <xsl:variable name="ref" select="@ref"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:group[@name=$ref]" mode="elements"/>
        </xsl:if>
        <xsl:call-template name="element-children"/>
        <!--group done-->
    </xsl:template>

    <xsl:template match="xsd:sequence" mode="elements">
        <!--sequence <xsl:value-of select="@name"/>-->
        <xsl:call-template name="element-children"/>
        <!--sequence done-->
    </xsl:template>

    <xsl:template match="xsd:choice" mode="elements">
        <!--choice <xsl:value-of select="@name"/>-->
        <xsl:call-template name="element-children"/>
        <!--choice done-->
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="elements">
        <!--complexType <xsl:value-of select="@name"/>-->
        <xsl:call-template name="element-children"/>
        <!--complexType done-->
    </xsl:template>

    <xsl:template match="xsd:complexContent" mode="elements">
        <!--complexContent <xsl:value-of select="@name"/>-->
        <xsl:call-template name="element-children"/>
        <!--complexContent done-->
    </xsl:template>

    <xsl:template match="xsd:extension" mode="elements">
        <!--extension <xsl:value-of select="@name"/>-->
        <xsl:variable name="base" select="@base"/>
        <xsl:apply-templates
                select="/xsd:schema/xsd:complexType[@name=$base]" mode="elements"/>
        <xsl:call-template name="element-children"/>
        <!--extension done-->
    </xsl:template>


    <!-- substitution -->

    <xsl:template match="xsd:element[@substitutionGroup]" mode="substitution">
        <xsl:variable name="sub" select="@substitutionGroup"/>
        <p>
            This element can be used as a substitute for
            &lt;<xsl:value-of select="$sub"/> ...&gt;
        </p>
        <xsl:apply-templates
                select="/xsd:schema/xsd:element[@name=$sub]" mode="documentation"/>
    </xsl:template>


    <!-- convert common types to nicer text -->

    <xsl:template name="rewrite-type">
        <xsl:param name="type"/>
        <xsl:variable name="simpleType">
            <xsl:choose>
                <xsl:when test="starts-with($type, 'mule:')"><xsl:value-of select="substring($type, 6)"/></xsl:when>
                <xsl:when test="starts-with($type, 'xsd:')"><xsl:value-of select="substring($type, 5)"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="$type"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$simpleType='substitutableInt'">integer</xsl:when>
            <xsl:when test="$simpleType='substitutableBoolean'">boolean</xsl:when>
            <xsl:when test="$simpleType='substitutableLong'">long</xsl:when>
            <xsl:when test="$simpleType='substitutablePortNumber'">port number</xsl:when>
            <xsl:when test="$simpleType='substitutableClass'">class name</xsl:when>
            <xsl:when test="$simpleType='substitutableName' or $simpleType='NMTOKEN' or $simpleType='IDREF'">name (no spaces)</xsl:when>
            <xsl:when test="$simpleType='nonBlankString'">name</xsl:when>
            <xsl:when test="$simpleType='NMTOKENS'">list of names</xsl:when>
            <xsl:otherwise><xsl:value-of select="$simpleType"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- add attribution -->

    <xsl:template name="attribution">
        <xsl:param name="text"/>
        <!-- <br/><small><em><xsl:value-of select="$text"/></em></small> -->
    </xsl:template>


    <!-- links via a separate index - see links.xml -->
    <!-- this includes a confluence specific hack - the link itself has the name in -->

    <xsl:template name="link">
        <xsl:param name="item"/>
        <xsl:variable name="page">
            <xsl:apply-templates select="$items-to-pages">
                <xsl:with-param name="item" select="$item"/>
            </xsl:apply-templates>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string-length($page) > 0">
                <xsl:variable name="pageClean" select="translate($page, '\+-:', '')"/>
                <xsl:variable name="itemClean" select="translate($item, '\+-:', '')"/>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$page"/>#<xsl:value-of select="$pageClean"/>-<xsl:value-of select="$itemClean"/>
                    </xsl:attribute>
                    <xsl:value-of select="$item"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$item"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="anchor">
        <xsl:param name="item"/>
        <xsl:variable name="page">
            <xsl:apply-templates select="$items-to-pages">
                <xsl:with-param name="item" select="$item"/>
            </xsl:apply-templates>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string-length($page) > 0">
                <xsl:variable name="pageClean" select="translate($page, '\+-:', '')"/>
                <xsl:variable name="itemClean" select="translate($item, '\+-:', '')"/>
                <xsl:value-of select="$pageClean"/>-<xsl:value-of select="$itemClean"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$item"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="links">
        <xsl:param name="item"/>
        <xsl:value-of select="key('item-to-page', $item)/page"/>
    </xsl:template>

</xsl:stylesheet>
        
