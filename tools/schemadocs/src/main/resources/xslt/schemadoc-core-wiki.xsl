<xsl:stylesheet
        version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
        >

    <!-- the base path mapping for snippet URLS.  These are configured in the Confluence Snippet macro -->
    <xsl:param name="snippetBase"/>
    <xsl:param name="topstylelevel" select="2"/>
    <xsl:variable name="topstyle" select="concat('&#10;h', $topstylelevel, '. ')"/>
    <xsl:variable name="nextstyle" select="concat('&#10;h', $topstylelevel + 1, '. ')"/>
    <xsl:param name="parentSchemaBase"/>
    <xsl:param name="parentSchema1"/>
    <xsl:param name="parentSchema2"/>
    <xsl:param name="parentSchema3"/>
    <xsl:param name="parentSchema4"/>
    <xsl:param name="parentSchema5"/>
    <xsl:variable name="fullParentSchema0">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/modules/spring-config/src/main/resources/META-INF/mule.xsd')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="fullParentSchema1">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase and $parentSchema1">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/', $parentSchema1)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$parentSchema1"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="fullParentSchema2">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase and $parentSchema2">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/', $parentSchema2)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$parentSchema2"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="fullParentSchema3">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase and $parentSchema3">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/', $parentSchema3)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$parentSchema3"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="fullParentSchema4">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase and $parentSchema4">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/', $parentSchema4)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$parentSchema4"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="fullParentSchema5">
        <xsl:choose>
            <xsl:when test="$parentSchemaBase and $parentSchema5">
                <xsl:value-of 
                     select="concat($parentSchemaBase, '/', $parentSchema5)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$parentSchema5"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="defaultSnippetBase">mule-2-current</xsl:variable>

    <!-- the table of pages for linking -->
    <xsl:key name="item-to-page" match="link" use="item"/>
    <xsl:variable name="items-to-pages"
                  select="document('http://svn.codehaus.org/mule/branches/mule-2.0.x/tools/schemadocs/src/main/resources/links.xml')/links"/>

    <xsl:output method="text" standalone="yes" indent="no"/>

    <xsl:template match="xsd:element" mode="single-element">

        <!--
        <xsl:value-of select=
          "concat('topStyle= ', $topstyle, ' nextstyle= ', $nextstyle,
                ' fullParentSchema0= ', $fullParentSchema0, 
                ' fullParentSchema1= ', $fullParentSchema1, 
                ' fullParentSchema2= ', $fullParentSchema2,
                ' fullParentSchema3= ', $fullParentSchema3, 
                ' fullParentSchema4= ', $fullParentSchema4,
                ' fullParentSchema5= ', $fullParentSchema5)"/>
        -->

        <xsl:variable name="temp" select="translate(@name, '-', ' ')"/>
        <xsl:variable name="t" select="concat( translate( substring( $temp, 1, 1 ),'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' ), substring( $temp, 2, string-length( $temp )))"/>

        <xsl:value-of select="$topstyle"/><xsl:value-of select="$t"/>
        <xsl:value-of select="xsd:annotation/xsd:documentation"/>

        <xsl:variable name="type"><xsl:value-of select="@type"/> </xsl:variable>

        <xsl:apply-templates select="//xsd:complexType[@name=$type]" mode="table">
            <xsl:with-param name="name"><xsl:value-of select="@name"/> </xsl:with-param>
        </xsl:apply-templates>

        <xsl:if test="@type">
            <xsl:value-of select="$nextstyle"/>Child Elements of &lt;<xsl:value-of select="@name"/>...&gt;
            ||Name||Cardinality||Description||
            <xsl:variable name="type" select="@type"/>
            <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$type]" mode="elements"/>
        </xsl:if>

        <!-- Render Example configurations -->
        <xsl:apply-templates select="xsd:annotation/xsd:appinfo/schemadoc:snippet"/>
    </xsl:template>

    <xsl:template match="xsd:complexType" mode="table">
        <xsl:param name="name"/>
        <!--
        <xsl:if test="(count(.//xsd:attribute) +  count(.//xsd:attributeGroup)) > 0">
        -->
        <xsl:value-of select="$nextstyle"/>Attributes of &lt;<xsl:value-of select="$name"/>...&gt;
        ||Name||Type||Required||Default||Description||
        <xsl:apply-templates select="." mode="attributes"/>
        <!-- </xsl:if> -->

        <!--
        <xsl:if test="(count(.//xsd:element) + count(.//xsd:choice)) > 0 ">
        h3. Child Elements of &lt;<xsl:value-of select="$name"/>...&gt;
        ||Name||Cardinality||Description||
        <xsl:call-template name="element-children"/>
        </xsl:if>
        -->
    </xsl:template>

    <!-- TRANSFORMERS -->
    <xsl:template name="transformers">

        <xsl:value-of select="$nextstyle"/> Transformers
        These are transformers specific to this transport. Note that these are added automatically to the Mule registry
        at start up. When doing automatic transformations these will be included when searching for the correct
        transformers.

        ||Name||Description||
        <xsl:apply-templates select="//xsd:element[contains(@substitutionGroup,'abstract-transformer')]"
                             mode="transformer"/>
    </xsl:template>

    <xsl:template match="xsd:element" mode="transformer">
        |<xsl:value-of select="@name"/>|<xsl:value-of select="normalize-space(xsd:annotation/xsd:documentation)"/>|
    </xsl:template>

    <!-- FILTERS -->
    <xsl:template name="filters">

        <xsl:value-of select="$nextstyle"/> Filters
        Filters can be used on inbound endpoints to control which data is received by a service.

        ||Name||Description||
        <xsl:apply-templates select="//xsd:element[contains(@substitutionGroup,'abstract-filter')]" mode="filter"/>
    </xsl:template>

    <xsl:template match="xsd:element" mode="filter">
        |<xsl:value-of select="@name"/>|<xsl:value-of select="normalize-space(xsd:annotation/xsd:documentation)"/>|
    </xsl:template>

    <!-- App Info extension processing -->
    <xsl:template match="xsd:appinfo/schemadoc:snippet">

        <xsl:variable name="snippet">
            <xsl:choose>
                <xsl:when test="$snippetBase">
                    <xsl:value-of select="$snippetBase"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$defaultSnippetBase"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="$nextstyle"/>Example Configurations
        <xsl:choose>
            <xsl:when test="string-length(.) > 0">
                <xsl:value-of select="."/>
            </xsl:when>
            <xsl:otherwise>
                Note that the documentation for this example should be embedded within the code.
            </xsl:otherwise>
        </xsl:choose>

        {expand}
        {snippet:lang=<xsl:value-of select="@lang"/>|id=<xsl:value-of select="@id"/>|url=<xsl:value-of
            select="$snippet"/>/<xsl:value-of select="@sourcePath"/>}
        {expand}
    </xsl:template>

    <!-- end AppInfo processng -->
    <!-- documentation

       we need to collect documentation from:
       - the ref
       - the element
       - the type
       we don't use extension or substitution here -->

    <xsl:template match="xsd:element[@ref]" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()"
                                 mode="copy"/>
            <xsl:call-template name="attribution">
                <xsl:with-param name="text">
                    From reference for element<xsl:value-of select="@ref"/>.
                </xsl:with-param>
            </xsl:call-template>

        </xsl:if>
        <xsl:variable name="ref" select="@ref"/>
        <xsl:apply-templates
                select="/xsd:schema/xsd:element[@name=$ref]" mode="documentation"/>
    </xsl:template>

    <xsl:template match="xsd:element[@name]" mode="documentation">
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()"
                                 mode="copy"/>
            <xsl:call-template name="attribution">
                <xsl:with-param name="text">
                    From declaration of element<xsl:value-of select="@name"/>.
                </xsl:with-param>
            </xsl:call-template>

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
            <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()"
                                 mode="copy"/>
            <xsl:call-template name="attribution">
                <xsl:with-param name="text">
                    <xsl:choose>
                        <xsl:when test="@name">
                            From declaration of type<xsl:value-of select="@name"/>.
                        </xsl:when>
                        <xsl:otherwise>
                            From type declaration.
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>


    <!-- attributes -->

    <xsl:template match="xsd:attribute[@name]" mode="attributes">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="string-length(@type)">
                    <xsl:call-template name="rewrite-type">
                        <xsl:with-param name="type" select="@type"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="xsd:simpleType/xsd:restriction/xsd:enumeration">
                    <xsl:for-each select="xsd:simpleType/xsd:restriction/xsd:enumeration">
                        <xsl:if test="@value">
                            <xsl:value-of select="@value"/>
                            <xsl:if test="position()!=last()">/</xsl:if>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="required">
            <xsl:choose>
                <xsl:when test="@use='required'">yes</xsl:when>
                <xsl:otherwise>no</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="default">
            <xsl:if test="@default">
                <xsl:value-of select="@default"/>
            </xsl:if>
        </xsl:variable>
        <xsl:variable name="doc">
            <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
                        <xsl:apply-templates
                                select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()"
                                mode="copy"/>
                </xsl:if>
            <!-- leave this line as-is -->
        </xsl:variable>|<xsl:value-of select="@name"/> |<xsl:value-of select="$type"/> |<xsl:value-of select="$required"/> |<xsl:value-of select="$default"/> |<xsl:value-of select="normalize-space($doc)"/>|
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

    <xsl:template name="attributes-from-parent-schema">
        <xsl:param name="parentSchema"/>
        <xsl:param name="typeLocalName"/>
        <xsl:param name="typeNamespace"/>

        <xsl:if test="$parentSchema">
            <xsl:if test="document($parentSchema)/*/@targetNamespace=$typeNamespace">
                <xsl:apply-templates 
                     select="document($parentSchema)/xsd:schema/xsd:complexType[@name=$typeLocalName]" 
                     mode="attributes"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template name="elements-from-parent-schema">
        <xsl:param name="parentSchema"/>
        <xsl:param name="typeLocalName"/>
        <xsl:param name="typeNamespace"/>
        <xsl:if test="$parentSchema">
            <xsl:if test="document($parentSchema)/*/@targetNamespace=$typeNamespace">
                <xsl:apply-templates 
                     select="document($parentSchema)/xsd:schema/xsd:complexType[@name=$typeLocalName]" 
                     mode="elements"/>
            </xsl:if>
        </xsl:if>
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
        <xsl:variable name="topElement" select="ancestor::xsd:schema"/>
        <xsl:variable name="schemaNs" select="$topElement/@targetNamespace"/>
        <xsl:variable name="prefix" select="substring-before($base, ':')"/>

        <xsl:variable name="local">
            <xsl:choose>
                <xsl:when test="contains($base, ':')">
                    <xsl:value-of 
                      select="substring-after($base, ':')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$base"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="itemNs">
            <xsl:choose>
                <xsl:when test="$prefix">
                    <xsl:value-of 
                      select="$topElement/namespace::*[local-name()=$prefix]"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$schemaNs"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="/*/@targetNamespace=$itemNs">
            <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$base]" mode="attributes"/>
        </xsl:if>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema0"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema1"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema2"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema3"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema4"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="attributes-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema5"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name="attribute-children"/>
    </xsl:template>


    <!-- child elements -->
    <!-- documentation here more restricted than "documentation" mode -->


    <xsl:template match="xsd:element[@ref]" mode="elements">
        <!-- cardinality i.e. minoccurs/maxoccurs -->
        <xsl:variable name="min">
            <xsl:choose>
                <xsl:when test="@minOccurs">
                    <xsl:value-of select="@minOccurs"/>
                </xsl:when>
                <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="max">
            <xsl:choose>
                <xsl:when test="@maxOccurs='unbounded'">*</xsl:when>
                <xsl:when test="@maxOccurs">
                    <xsl:value-of select="@maxOccurs"/>
                </xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="ref" select="@ref"/>
        <xsl:variable name="doc">
            <xsl:apply-templates select="." mode="elements-doc"/>
                <xsl:apply-templates
                        select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-doc"/>
                <xsl:apply-templates
                        select="/xsd:schema/xsd:element[@name=$ref]" mode="elements-abstract"/>
        </xsl:variable>|<xsl:value-of select="@ref"/> |<xsl:value-of select="$min"/>..<xsl:value-of select="$max"/> |<xsl:value-of select="normalize-space($doc)"/> |
    </xsl:template>

    <xsl:template match="xsd:element[contains(@name, ':abstract-')]"
                  mode="elements-abstract">
        <!--element (abstract) <xsl:value-of select="@name"/>-->
        <xsl:variable name="name" select="@name"/>
        <xsl:choose>
            <!-- this should always be true when using the normalized schema -->
            <xsl:when test="/xsd:schema/xsd:element[@substitutionGroup=$name]">
                The following elements can be used here:

                <xsl:apply-templates
                        select="/xsd:schema/xsd:element[@substitutionGroup=$name]"
                        mode="elements-list"/>

            </xsl:when>
            <xsl:otherwise>
                This is an abstract element; another element with a compatible
                type must be used in its place. However, no replacements were
                found when generating this documentation.
            </xsl:otherwise>
        </xsl:choose>

        <!--element (abstract) done-->
    </xsl:template>

    <!-- otherwise, do nothing -->
    <xsl:template match="xsd:element" mode="elements-abstract"/>

    <xsl:template match="xsd:element[@name]" mode="elements-list">
        <!--element (list) <xsl:value-of select="@name"/>-->
          * <xsl:call-template name="link">
            <xsl:with-param name="item">
                <xsl:value-of select="@name"/>
            </xsl:with-param>
        </xsl:call-template>
        <!-- li>&lt;<xsl:value-of select="@name"/> ...&gt;</li -->
        <!--element (list) done-->
    </xsl:template>

    <xsl:template match="xsd:element" mode="elements-doc">
        <!--element (doc) <xsl:value-of select="@name"/>-->
        <xsl:if test="xsd:annotation/xsd:documentation/text()|xsd:annotation/xsd:documentation/*">
            <xsl:apply-templates select="xsd:annotation/xsd:documentation/*|xsd:annotation/xsd:documentation/text()"
                                 mode="copy"/>
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
        <!--<xsl:variable name="name">-->
        <!--<xsl:call-template name="link">-->
        <!--<xsl:with-param name="item">-->
        <!--<xsl:value-of select="@name"/>-->
        <!--</xsl:with-param>-->
        <!--</xsl:call-template>-->
        <!--</xsl:variable>-->

        <!-- cardinality i.e. minoccurs/maxoccurs -->
        <xsl:variable name="min">
            <xsl:choose>
                <xsl:when test="@minOccurs">
                    <xsl:value-of select="@minOccurs"/>
                </xsl:when>
                <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="max">
            <xsl:choose>
                <xsl:when test="@maxOccurs='unbounded'">*</xsl:when>
                <xsl:when test="@maxOccurs">
                    <xsl:value-of select="@maxOccurs"/>
                </xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="ref" select="@ref"/>
        <xsl:variable name="doc">
            <xsl:apply-templates select="." mode="elements-doc"/>
        </xsl:variable>| <xsl:value-of select="@name"/>| <xsl:value-of select="$min"/>..<xsl:value-of select="$max"/>| <xsl:value-of select="normalize-space($doc)"/>|
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
        <xsl:variable name="topElement" select="ancestor::xsd:schema"/>
        <xsl:variable name="schemaNs" select="$topElement/@targetNamespace"/>
        <xsl:variable name="prefix" select="substring-before($base, ':')"/>

        <xsl:variable name="local">
            <xsl:choose>
                <xsl:when test="contains($base, ':')">
                    <xsl:value-of 
                      select="substring-after($base, ':')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$base"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="itemNs">
            <xsl:choose>
                <xsl:when test="$prefix">
                    <xsl:value-of 
                      select="$topElement/namespace::*[local-name()=$prefix]"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$schemaNs"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="/*/@targetNamespace=$itemNs">
            <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$base]" mode="elements"/>
        </xsl:if>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema0"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema1"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema2"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema3"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema4"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name ="elements-from-parent-schema">
            <xsl:with-param name="parentSchema" select="$fullParentSchema5"/> 
            <xsl:with-param name="typeLocalName" select="$local"/> 
            <xsl:with-param name="typeNamespace" select="$itemNs"/> 
        </xsl:call-template>
        <xsl:call-template name="element-children"/>
        <!--extension done-->
    </xsl:template>


    <!-- substitution -->

    <xsl:template match="xsd:element[@substitutionGroup]" mode="substitution">
        <xsl:variable name="sub" select="@substitutionGroup"/>
            This element can be used as a substitute for
            &lt;
            <xsl:value-of select="$sub"/>
            ...&gt;
        <xsl:apply-templates
                select="/xsd:schema/xsd:element[@name=$sub]" mode="documentation"/>
    </xsl:template>


    <!-- convert common types to nicer text -->

    <xsl:template name="rewrite-type">

        <xsl:param name="type"/>
        <xsl:variable name="simpleType">
            <xsl:choose>
                <xsl:when test="starts-with($type, 'mule:')">
                    <xsl:value-of select="substring($type, 6)"/>
                </xsl:when>
                <xsl:when test="starts-with($type, 'xsd:')">
                    <xsl:value-of select="substring($type, 5)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$type"/>
                </xsl:otherwise>
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
            <xsl:otherwise>
                <xsl:value-of select="$simpleType"/>
            </xsl:otherwise>
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
                        <xsl:value-of select="$page"/>#<xsl:value-of select="$pageClean"/>-
                        <xsl:value-of select="$itemClean"/>
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
                <xsl:value-of select="$pageClean"/>-
                <xsl:value-of select="$itemClean"/>
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
        
