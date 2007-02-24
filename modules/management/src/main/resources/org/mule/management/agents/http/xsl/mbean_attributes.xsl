<?xml version="1.0"?>
<!--
 $Id$
 
 Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 
 The software in this package is published under the terms of the MuleSource MPL
 license, a copy of which has been included with this distribution in the
 LICENSE.txt file.
 -->
<!--
 Copyright (C) The MX4J Contributors.
 All rights reserved.

 This software is distributed under the terms of the MX4J License version 1.0.
 See the terms of the MX4J License in the documentation provided with this software.

 Author: Carlos Quiroz (tibu@users.sourceforge.net)
 Revision: 1.2
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <!-- array link generator -->
   <xsl:template name="array">

      <xsl:choose>
         <xsl:when test="@isnull='false'">
            <xsl:variable name="url" select="concat('getattribute?objectname=', ../@objectname, '&amp;attribute=', @name, '&amp;format=array&amp;template=viewarray')"/>
            <a href="{$url}">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">mbean_attributes.array.view</xsl:with-param>
               </xsl:call-template>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.array.null</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Collection link generator -->
   <xsl:template name="collection">

      <xsl:choose>
         <xsl:when test="@isnull='false'">
            <xsl:variable name="url" select="concat('getattribute?objectname=', ../@objectname, '&amp;attribute=', @name, '&amp;format=collection&amp;template=viewcollection')"/>
            <a href="{$url}">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">mbean_attributes.collection.view</xsl:with-param>
               </xsl:call-template>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.collection.null</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Map link generator -->
   <xsl:template name="map">

      <xsl:choose>
         <xsl:when test="@isnull='false'">
            <xsl:variable name="url" select="concat('getattribute?objectname=', ../@objectname, '&amp;attribute=', @name, '&amp;format=map&amp;template=viewmap')"/>
            <a href="{$url}">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">mbean_attributes.map.view</xsl:with-param>
               </xsl:call-template>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.map.null</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Composite data -->
   <xsl:template name="compositedata">

      <xsl:choose>
         <xsl:when test="@isnull='false'">
            <xsl:variable name="url" select="concat('getattribute?objectname=', ../@objectname, '&amp;attribute=', @name, '&amp;format=compositedata&amp;template=identity')"/>
            <a href="{$url}">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">mbean_attributes.compositedata.view</xsl:with-param>
               </xsl:call-template>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.compositedata.null</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Composite data -->
   <xsl:template name="tabulardata">

      <xsl:choose>
         <xsl:when test="@isnull='false'">
            <xsl:variable name="url" select="concat('getattribute?objectname=', ../@objectname, '&amp;attribute=', @name, '&amp;format=tabulardata&amp;template=viewtabulardata')"/>
            <a href="{$url}">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">mbean_attributes.tabulardata.view</xsl:with-param>
               </xsl:call-template>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.tabulardata.null</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="raw-input">
      <xsl:param name="type"/>
      <xsl:param name="value"/>
      <xsl:param name="name"/>
      <xsl:param name="strinit"/>

      <xsl:variable name="result">none</xsl:variable>
      <xsl:choose>
         <xsl:when test="$type='java.lang.Boolean' or $type='boolean'">
            <xsl:choose>
               <xsl:when test="$value='true'">
                  <input name="{$name}" type="radio" checked="checked" value="true">true </input>
                  <input name="{$name}" type="radio" value="false">false </input>
               </xsl:when>
               <xsl:when test="$value='false'">
                  <input name="{$name}" type="radio" value="true">true </input>
                  <input name="{$name}" type="radio" checked="checked" value="false">false </input>
               </xsl:when>
               <xsl:otherwise>
                  <input name="{$name}" type="radio" value="true">true </input>
                  <input name="{$name}" type="radio" value="false">false </input>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:otherwise>
            <input type="text" name="{$name}" value="{$value}"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template match="Attribute[@type]" name="form">
      <xsl:param name="value"/>
      <xsl:choose>
         <xsl:when test="@strinit='true'">
            <xsl:variable name="name" select="@name"/>
            <xsl:call-template name="raw-input">
               <xsl:with-param name="type" select="@type"/>
               <xsl:with-param name="value" select="$value"/>
               <xsl:with-param name="name">
                  <xsl:value-of select="concat('value_', @name)"/>
               </xsl:with-param>
               <xsl:with-param name="strinit">true</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="submit">
               <xsl:with-param name="name" select="@name"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.form.unknowntype</xsl:with-param>
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!-- Makes the submit button for setting one attribute -->
   <xsl:template match="Attribute[@type]" name="submit">
      <xsl:param name="name"/>
      <xsl:if test="@strinit='true'">
         <xsl:variable name="str.set">
            <xsl:call-template name="str">
               <xsl:with-param name="id">mbean_attributes.submit.set</xsl:with-param>
            </xsl:call-template>
         </xsl:variable>
         <input type="Submit" name="set_{$name}" value="{$str.set}"/>
      </xsl:if>
   </xsl:template>

   <!-- makes a link for objectnames from current value element -->
   <xsl:template name="objectnamevalue">
      <xsl:call-template name="renderobject">
         <xsl:with-param name="objectclass">javax.management.ObjectName</xsl:with-param>
         <xsl:with-param name="objectvalue" select="@value"/>
      </xsl:call-template>
   </xsl:template>

   <!-- Renders an object
      Currently transforms javax.management.ObjectName to links
      Renders others as strings -->
   <xsl:template name="renderobject">
      <xsl:param name="objectclass"/>
      <xsl:param name="objectvalue"/>
      <xsl:choose>
         <xsl:when test="$objectclass='javax.management.ObjectName'">
            <xsl:variable name="name_encoded">
               <xsl:call-template name="uri-encode">
                  <xsl:with-param name="uri">
                     <xsl:value-of select="$objectvalue"/>
                  </xsl:with-param>
               </xsl:call-template>
            </xsl:variable>
            <a href="/mbean?objectname={$name_encoded}">
               <xsl:value-of select="$objectvalue"/>
            </a>
         </xsl:when>
         <xsl:otherwise>
            <!-- Use the following line when the result of an invocation
            returns e.g. HTML or XML data
            <xsl:value-of select="$objectvalue" disable-output-escaping="true" />
            -->
            <xsl:value-of select="$objectvalue"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template match="Attribute" name="WO">
      <td align="right" class="mbean_row">
         <xsl:call-template name="str">
            <xsl:with-param name="id">mbean_attributes.WO.readonly</xsl:with-param>
         </xsl:call-template>
      </td>
      <td align="right" class="mbean_row">
         <xsl:call-template name="form"/>
      </td>
   </xsl:template>

   <!-- Template for readwrite attributes -->
   <xsl:template match="Attribute" name="RW">
      <td align="right" class="mbean_row">
         <xsl:choose>
            <xsl:when test="@aggregation='collection'">
               <xsl:call-template name="collection"/>
            </xsl:when>
            <xsl:when test="@aggregation='map'">
               <xsl:call-template name="map"/>
            </xsl:when>
            <xsl:when test="starts-with(@type, '[')">
               <xsl:call-template name="array"/>
            </xsl:when>
            <xsl:when test="@type='javax.management.ObjectName'">
               <xsl:call-template name="objectnamevalue"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="@value"/>
            </xsl:otherwise>
         </xsl:choose>
      </td>
      <td align="right" class="mbean_row">
         <xsl:call-template name="form">
            <xsl:with-param name="value" select="@value"/>
         </xsl:call-template>
      </td>
   </xsl:template>

   <!-- Template for readonly attributes -->
   <xsl:template match="Attribute" name="RO">
      <td align="right" class="mbean_row">
         <xsl:choose>
            <xsl:when test="@aggregation='collection'">
               <xsl:call-template name="collection"/>
            </xsl:when>
            <xsl:when test="@aggregation='map'">
               <xsl:call-template name="map"/>
            </xsl:when>
            <xsl:when test="starts-with(@type, '[')">
               <xsl:call-template name="array"/>
            </xsl:when>
            <xsl:when test="@type='javax.management.ObjectName'">
               <xsl:call-template name="objectnamevalue"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="@value"/>
            </xsl:otherwise>
         </xsl:choose>
      </td>
      <td align="right" class="mbean_row">
         <xsl:call-template name="str">
            <xsl:with-param name="id">mbean_attributes.RO.readonly</xsl:with-param>
         </xsl:call-template>
      </td>
   </xsl:template>

   <!-- MBean's attributes template -->
   <xsl:template name="attribute">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
         <tr class="darkline">
            <td width="20%" align="left" class="darkline">
               <div class="tableheader">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.name</xsl:with-param>
                  </xsl:call-template>
               </div>
            </td>
            <td width="20%" align="left" class="darkline">
               <div class="tableheader">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.description</xsl:with-param>
                  </xsl:call-template>
               </div>
            </td>
            <td width="20%" align="left" class="darkline">
               <div class="tableheader">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.type</xsl:with-param>
                  </xsl:call-template>
               </div>
            </td>
            <td width="20%" align="right" class="darkline">
               <div class="tableheader">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.value</xsl:with-param>
                  </xsl:call-template>
               </div>
            </td>
            <td width="*" align="right" class="darkline">
               <div class="tableheader">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.newvalue</xsl:with-param>
                  </xsl:call-template>
               </div>
            </td>
         </tr>
         <form action="setattributes" method="get">
            <xsl:for-each select="Attribute">
               <xsl:sort data-type="text" order="ascending" select="@name"/>
               <xsl:variable name="classtype">
                  <xsl:if test="(position() mod 2)=1">clearline</xsl:if>
                  <xsl:if test="(position() mod 2)=0">darkline</xsl:if>
               </xsl:variable>
               <tr class="{$classtype}">
                  <td class="mbean_row">
                     <xsl:value-of select="@name"/>
                  </td>
                  <td class="mbean_row">
                     <xsl:value-of select="@description"/>
                  </td>
                  <td class="mbean_row">
                     <xsl:choose>
                        <xsl:when test="starts-with(@type, '[L')">
                           <xsl:call-template name="str">
                              <xsl:with-param name="id">mbean_attributes.attribute.arrayof</xsl:with-param>
                              <xsl:with-param name="p0">
                                 <xsl:value-of select="substring-before(substring-after(@type, '[L'), ';')"/>
                              </xsl:with-param>
                           </xsl:call-template>
                        </xsl:when>
                        <xsl:when test="starts-with(@type, '[')">
                           <xsl:variable name="type" select="substring-after(@type, '[')"/>
                           <xsl:call-template name="str">
                              <xsl:with-param name="id">mbean_attributes.attribute.arrayof</xsl:with-param>
                              <xsl:with-param name="p0">
                                 <xsl:choose>
                                    <xsl:when test="$type = 'Z'">boolean</xsl:when>
                                    <xsl:when test="$type = 'C'">char</xsl:when>
                                    <xsl:when test="$type = 'F'">float</xsl:when>
                                    <xsl:when test="$type = 'D'">double</xsl:when>
                                    <xsl:when test="$type = 'B'">byte</xsl:when>
                                    <xsl:when test="$type = 'S'">short</xsl:when>
                                    <xsl:when test="$type = 'I'">int</xsl:when>
                                    <xsl:when test="$type = 'J'">long</xsl:when>
                                 </xsl:choose>
                              </xsl:with-param>
                           </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                           <xsl:value-of select="@type"/>
                        </xsl:otherwise>
                     </xsl:choose>
                  </td>
                  <xsl:choose>
                     <xsl:when test="@availability='RO'">
                        <xsl:call-template name="RO"/>
                     </xsl:when>
                     <xsl:when test="@availability='RW'">
                        <xsl:call-template name="RW"/>
                     </xsl:when>
                     <xsl:when test="@availability='WO'">
                        <xsl:call-template name="WO"/>
                     </xsl:when>
                  </xsl:choose>
               </tr>
            </xsl:for-each>
            <td colspan="5" align="right" class="attributes_setall">
               <input type="hidden" name="objectname" value="{$request.objectname}"/>
               <xsl:variable name="str.setall">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">mbean_attributes.attribute.setall</xsl:with-param>
                  </xsl:call-template>
               </xsl:variable>
               <input type="Submit" name="setall" value="{$str.setall}"/>
            </td>
         </form>
      </table>
   </xsl:template>
</xsl:stylesheet>

