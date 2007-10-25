<xsl:stylesheet 
        version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        >

    <!-- $Id$ -->

    <!-- this generates html documentation for a particular, named element
         using the information within a single schema.

         a lot of information is available and comments can be found in
         many related areas of the schema; this tries to give a fairly
         comprehensive summary without too much duplication.

         a lot of things could be improved.  obvious targets include:
         - cross-linking
         - using multiple schema

         some features commented-out to give a "friendlier" interface.
    -->

    <xsl:output method="html"/>
    <xsl:param name="elementName"/>

    <xsl:template match="/">
        <html>
            <body>
                <xsl:apply-templates
                        select="//xsd:element[@name=$elementName]" mode="start"/>
            </body>
        </html>
    </xsl:template>
  
    <xsl:template match="xsd:element" mode="start">
        <h2>&lt;<xsl:value-of select="@name"/> ...&gt;</h2>
        <xsl:apply-templates select="." mode="documentation"/>
        <!--
        <xsl:if test="@type">
            <p>Type: <xsl:value-of select="@type"/></p>
        </xsl:if>
        -->
         <h3>Properties</h3>
        <table style="border: 2px solid black; empty-cells: show; border-collapse: collapse">
            <tr>
                <th rowspan="2" style="width:25%; border-bottom: 1px solid black; border-right: 1px solid black">Name</th>
                <th style="width:25%">Type</th>
                <th style="width:25%">Required</th>
                <th style="width:25%">Default</th>
            </tr>
            <tr>
                <th colspan="3" style="border-bottom: 1px solid black">Description</th>
            </tr>
            <xsl:apply-templates select="." mode="attributes"/>
        </table>
        <h3>Child Elements</h3>
        <table style="border: 2px solid black; empty-cells: show; border-collapse: collapse">
            <tr>
                <th rowspan="2" style="width:25%; border-bottom: 1px solid black; border-right: 1px solid black">Name</th>
                <th>Type</th>
            </tr>
            <tr>
                <th style="border-bottom: 1px solid black">Description</th>
            </tr>
            <xsl:call-template name="element-children"/>
            <xsl:if test="@type">
                <xsl:variable name="type" select="@type"/>
                <xsl:apply-templates
                        select="/xsd:schema/xsd:complexType[@name=$type]" mode="elements"/>
            </xsl:if>
        </table>
        <!--
        <h3>Substitution</h3>
        <xsl:apply-templates select="." mode="substitution"/>
        <h3>Type Hierarchy</h3>
        <ul>
            <xsl:apply-templates select="." mode="types"/>
        </ul>
        -->
    </xsl:template>


    <!-- documentation

       we need to collect documentation from:
       - the ref
       - the element
       - the type
       we don't use extension or substitution here -->

    <xsl:template match="xsd:element[@ref]" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()">
            <p>
                <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
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
        <xsl:if test="xsd:annotation/xsd:documentation/text()">
            <p>
                <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
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
                    select="xsd:complexType[@name=$type]" mode="documentation"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()">
            <p>
                <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
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
            <td rowspan="2" style="border-bottom: 1px solid black; border-right: 1px solid black"><xsl:value-of select="@name"/></td>
            <td style="text-align: center"><xsl:call-template name="rewrite-type"><xsl:with-param name="type" select="@type"/></xsl:call-template></td>
            <td style="text-align: center">
                <xsl:choose>
                    <xsl:when test="@required">required</xsl:when>
                    <xsl:otherwise>not required</xsl:otherwise>
                </xsl:choose>
            </td>
            <td style="text-align: center">
                <xsl:if test="@default"><xsl:value-of select="@default"/></xsl:if>
            </td>
        </tr>
        <tr style="border-bottom: 1px solid black">
            <td colspan="3">
                <xsl:if test="xsd:annotation/xsd:documentation/text()">
                    <p>
                        <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
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
        <xsl:apply-templates
                select="/xsd:schema/xsd:complexType[@name=$base]" mode="attributes"/>
        <xsl:call-template name="attribute-children"/>
    </xsl:template>

  
    <!-- child elements -->
    <!-- documentation here more restricted than "documentation" mode -->

    <xsl:template match="xsd:element[@ref]" mode="elements">
        <xsl:variable name="ref" select="@ref"/>
        <tr>
            <td rowspan="2" style="border-bottom: 1px solid black; border-right: 1px solid black"><xsl:value-of select="@ref"/></td>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-type"/>
        </tr>
        <tr><td style="border-bottom: 1px solid black">
            <!-- include both ref and element doc -->
            <xsl:apply-templates select="." mode="elements-doc"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-doc"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-abstract"/>
        </td></tr>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="elements">
        <tr>
            <td rowspan="2" style="border-bottom: 1px solid black; border-right: 1px solid black"><xsl:value-of select="@name"/></td>
            <xsl:apply-templates select="." mode="elements-type"/>
        </tr>
        <tr><td style="border-bottom: 1px solid black">
            <xsl:apply-templates select="." mode="elements-doc"/>
            <xsl:apply-templates select="." mode="elements-abstract"/>
        </td></tr>
    </xsl:template>

    <xsl:template match="xsd:element[contains(@name, ':abstract-')]"
                  mode="elements-abstract">
        <xsl:variable name="name" select="@name"/>
        <p>
            <xsl:choose>
                <xsl:when test="/xsd:schema/xsd:element[@substitutionGroup=$name]">
                    This is an abstract element; another element with a compatible
                    type must be used in its place.  The following is a list of
                    suitable replacements from this schema.  Other schema may
                    contain further possible elements.
                    <ul>
                        <xsl:apply-templates
                                select="/xsd:schema/xsd:element[@substitutionGroup=$name]"
                                mode="elements-list"/>
                    </ul>
                </xsl:when>
                <xsl:otherwise>
                    This is an abstract element; another element with a compatible
                    type must be used in its place.  No such types are defined in
                    this schema.  You will need to check other schema for suitable
                    elements.
                </xsl:otherwise>
            </xsl:choose>
        </p>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="elements-type">
        <td><xsl:value-of select="@type"/></td>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="elements-list">
        <li>&lt;<xsl:value-of select="@name"/> ...&gt;</li>
    </xsl:template>

    <xsl:template match="xsd:element" mode="elements-doc">
        <xsl:if test="xsd:annotation/xsd:documentation/text()">
            <p>
                <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template name="element-children">
        <xsl:apply-templates select="xsd:element" mode="elements"/>
        <xsl:apply-templates select="xsd:group" mode="elements"/>
        <xsl:apply-templates select="xsd:sequence" mode="elements"/>
        <xsl:apply-templates select="xsd:choice" mode="elements"/>
        <xsl:apply-templates select="xsd:complexType" mode="elements"/>
        <xsl:apply-templates select="xsd:complexContent" mode="elements"/>
        <xsl:apply-templates select="xsd:extension" mode="elements"/>
    </xsl:template>

    <xsl:template match="xsd:group" mode="elements">
        <xsl:if test="@ref">
            <xsl:variable name="ref" select="@ref"/>
            <xsl:apply-templates
                    select="/xsd:schema/xsd:group[@name=$ref]" mode="elements"/>
        </xsl:if>
        <xsl:call-template name="element-children"/>
    </xsl:template>

    <xsl:template match="xsd:sequence" mode="elements">
        <xsl:call-template name="element-children"/>
    </xsl:template>

    <xsl:template match="xsd:choice" mode="elements">
        <xsl:call-template name="element-children"/>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="elements">
        <xsl:call-template name="element-children"/>
    </xsl:template>

    <xsl:template match="xsd:complexContent" mode="elements">
        <xsl:call-template name="element-children"/>
    </xsl:template>

    <xsl:template match="xsd:extension" mode="elements">
        <xsl:variable name="base" select="@base"/>
        <xsl:apply-templates
                select="/xsd:schema/xsd:complexType[@name=$base]" mode="elements"/>
        <xsl:call-template name="element-children"/>
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


    <!-- types -->

    <xsl:template match="xsd:element[@type]" mode="types">
        <xsl:variable name="type" select="@type"/>
        <xsl:choose>
            <xsl:when test="/xsd:schema/xsd:complexType[@name=$type]">
                <xsl:apply-templates
                        select="/xsd:schema/xsd:complexType[@name=$type]" mode="types"/>
            </xsl:when>
            <xsl:otherwise>
                <li><xsl:value-of select="@type"/>
                    (This type is not defined in this schema.  This means that
                    other attributes and elements may exist which are not documented
                    here)</li>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
      
    <xsl:template match="xsd:element[./xsd:complexType]" mode="types">
        <xsl:apply-templates select="xsd:complexType"/>
    </xsl:template>
      
    <xsl:template name="type-children">
        <xsl:apply-templates select="xsd:complexType" mode="types"/>
        <xsl:apply-templates select="xsd:complexContent" mode="types"/>
        <xsl:apply-templates select="xsd:extension" mode="types"/>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="types">
        <li><xsl:value-of select="@name"/></li>
        <xsl:call-template name="type-children"/>
    </xsl:template>

    <xsl:template match="xsd:complexContent" mode="types">
        <xsl:call-template name="type-children"/>
    </xsl:template>

    <xsl:template match="xsd:extension" mode="types">
        <xsl:variable name="base" select="@base"/>
        <xsl:apply-templates
                select="/xsd:schema/xsd:complexType[@name=$base]" mode="types"/>
    </xsl:template>


    <!-- convert common types to nicer text -->

    <xsl:template name="rewrite-type">
        <xsl:param name="type"/>
        <xsl:choose>
            <xsl:when test="$type='mule:substitutableInt'">integer</xsl:when>
            <xsl:when test="$type='mule:substitutableBoolean'">boolean</xsl:when>
            <xsl:when test="$type='mule:substitutableLong'">long</xsl:when>
            <xsl:when test="$type='mule:substitutablePortNumber'">port number</xsl:when>
            <xsl:when test="$type='mule:substitutableClass'">class name</xsl:when>
            <xsl:otherwise><xsl:value-of select="$type"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
  
    <xsl:template name="attribution">
        <xsl:param name="text"/>
        <!-- <br/><small><em><xsl:value-of select="$text"/></em></small> -->
    </xsl:template>

</xsl:stylesheet>
