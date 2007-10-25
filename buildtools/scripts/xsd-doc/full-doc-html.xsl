<xsl:stylesheet 
   version="1.0" 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
   >

    <!-- an initial experiment at generating a "complete" doc
         outpu way too complex, problems with loops etc -->

  <xsl:output method="html"/>

  <xsl:template match="/*">
    <table border="1" width="100%">
      <xsl:apply-templates select="xsd:element[@name='mule']"/>
    </table>
  </xsl:template>
  
  
  <xsl:template match="xsd:element[@name]">
    <xsl:variable name="elementName" select="@name"/>
    <xsl:comment>start element <xsl:value-of select="$elementName"/></xsl:comment>
    <xsl:if test="starts-with($elementName, 'abstract-')">
      <xsl:apply-templates select="/xsd:schema/xsd:element[@substitutionGroup=$elementName]"/>
    </xsl:if>
    <xsl:apply-templates select="." mode="row1"/>
    <xsl:apply-templates select="." mode="row2"/>
    <xsl:apply-templates select="." mode="children"/>
    <xsl:comment>end element <xsl:value-of select="$elementName"/></xsl:comment>
  </xsl:template>
  
  <xsl:template match="xsd:element[@ref]">
    <xsl:variable name="elementRef" select="@ref"/>
    <xsl:variable name="refDoc"
                  select="xsd:annotation/xsd:documentation/text()"/>
    <xsl:comment>start element reference <xsl:value-of select="$elementRef"/></xsl:comment>
    <xsl:if test="starts-with($elementRef, 'abstract-')">
      <xsl:apply-templates select="/xsd:schema/xsd:element[@substitutionGroup=$elementRef]"/>
    </xsl:if>
    <xsl:if test="/xsd:schema/xsd:element[@name=$elementRef]">
      <xsl:apply-templates select="/xsd:schema/xsd:element[@name=$elementRef]" mode="row1"/>
      <xsl:choose>
        <xsl:when test="$refDoc">
          <tr><td colspan="2"><xsl:value-of select="$refDoc"/></td></tr>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="/xsd:schema/xsd:element[@name=$elementRef]" mode="row2"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="/xsd:schema/xsd:element[@name=$elementRef]" mode="children"/>
    </xsl:if>
    <xsl:comment>end element reference <xsl:value-of select="$elementRef"/></xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:element[@name]" mode="row1">
    <xsl:variable name="elementName" select="@name"/>
    <xsl:variable name="elementType" select="@type"/>
    <tr>
      <td class="name"><xsl:value-of select="$elementName"/></td>
      <td class="type"><xsl:value-of select="$elementType"/></td>
    </tr>
  </xsl:template>
    
  <xsl:template match="xsd:element[@name]" mode="row2">
    <tr>
      <td colspan="2">
        <xsl:choose>
          <xsl:when test="xsd:annotation/xsd:documentation/text()">
            <xsl:value-of select="xsd:annotation/xsd:documentation/text()"/>
          </xsl:when>
          <xsl:when test="@type">
            <xsl:variable name="elementType" select="@type"/>
            <xsl:value-of select="/xsd:schema/xsd:complextType[@name=$elementType]/xsd:annotation/xsd:documentation/text()"/>
          </xsl:when>
          <xsl:when test="xsd:complextType">
            <xsl:value-of select="xsd:complexType/xsd:annotation/xsd:documentation/text()"/>
          </xsl:when>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="xsd:element[@name]" mode="children">
    <xsl:variable name="elementType" select="@type"/>
    <!-- avoid recursion on nested filters -->
    <xsl:if test="not(ends-with($elementType, 'FilterType'))">
      <tr>
        <td colspan="3"><table border="1" width="100%">
            <td class="indent">&#32;</td>
            <td><table border="1" width="100%">
                <xsl:call-template name="children"/>
                <xsl:apply-templates select="/xsd:schema/xsd:complexType[@name=$elementType]"/>
            </table></td>
        </table></td>
      </tr>
    </xsl:if>
  </xsl:template>


  <xsl:template name="children">
    <xsl:apply-templates select="xsd:element"/>
    <xsl:apply-templates select="xsd:group"/>
    <xsl:apply-templates select="xsd:sequence"/>
    <xsl:apply-templates select="xsd:choice"/>
    <xsl:apply-templates select="xsd:complexType"/>
    <xsl:apply-templates select="xsd:complexContent"/>
    <xsl:apply-templates select="xsd:extension"/>
  </xsl:template>

  <xsl:template match="xsd:complexType">
    <xsl:variable name="typeName" select="@name"/>
    <xsl:comment>start complexType <xsl:value-of select="$typeName"/></xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end complexType <xsl:value-of select="$typeName"/></xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:complexContent">
    <xsl:comment>start complexContent</xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end complexContent</xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:extension">
    <xsl:comment>start extension</xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end extension</xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:sequence">
    <xsl:comment>start sequence</xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end sequence</xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:group[@ref]">
    <xsl:variable name="groupRef" select="@ref"/>
    <xsl:comment>start group reference <xsl:value-of select="$groupRef"/></xsl:comment>
    <xsl:apply-templates select="/xsd:schema/xsd:group[@name=$groupRef]"/>
    <xsl:comment>end group reference <xsl:value-of select="$groupRef"/></xsl:comment>
  </xsl:template>

  <xsl:template match="xsd:group">
    <xsl:variable name="groupName" select="@name"/>
    <xsl:comment>start group <xsl:value-of select="$groupName"/></xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end group <xsl:value-of select="$groupName"/></xsl:comment>
  </xsl:template>

  <!-- currently we do not mark choices.
       i did try doing so, but we have nested choices in places, which
       looked bad, and removing the nesting was a problem (using modes
       conflicted with reference handling) -->
  <xsl:template match="xsd:choice">
    <xsl:comment>start choice</xsl:comment>
    <xsl:call-template name="children"/>
    <xsl:comment>end choice</xsl:comment>
  </xsl:template>

</xsl:stylesheet>
