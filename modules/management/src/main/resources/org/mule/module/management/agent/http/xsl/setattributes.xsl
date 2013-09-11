<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
   <xsl:param name="html.stylesheet.type">text/css</xsl:param>
   <xsl:param name="head.title">setattributes.title</xsl:param>
   <xsl:include href="common.xsl"/>

   <!-- Request parameters -->
   <xsl:param name="request.objectname"/>
   <xsl:param name="request.attribute"/>
   <xsl:param name="request.value"/>

   <xsl:template name="operation">
      <xsl:for-each select="Operation">
         <table width="100%" cellpadding="0" cellspacing="0" border="0">

            <tr>
               <td width="100%" class="page_title">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">setattributes.operation.title</xsl:with-param>
                     <xsl:with-param name="p0">
                        <xsl:value-of select="$request.objectname"/>
                     </xsl:with-param>
                  </xsl:call-template>

               </td>
            </tr>
            <xsl:for-each select="//Attribute">
               <xsl:variable name="classtype">
                  <xsl:if test="(position() mod 2)=1">darkline</xsl:if>
                  <xsl:if test="(position() mod 2)=0">clearline</xsl:if>
               </xsl:variable>
               <xsl:if test="@result='success'">
                  <tr class="{$classtype}">
                     <td>
                        <div class="tableheader">
                           <!-- This would be better with a 2 param replacement -->
                           <xsl:call-template name="str">
                              <xsl:with-param name="id">setattributes.operation.success</xsl:with-param>
                              <xsl:with-param name="p0">
                                 <xsl:value-of select="@attribute"/>
                              </xsl:with-param>
                              <xsl:with-param name="p1">
                                 <xsl:value-of select="@value"/>
                              </xsl:with-param>
                           </xsl:call-template>
                        </div>
                     </td>
                  </tr>
               </xsl:if>
               <xsl:if test="@result='error'">
                  <tr class="{$classtype}">
                     <td>
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">setattributes.operation.error</xsl:with-param>
                           <xsl:with-param name="p0">
                              <xsl:value-of select="@attribute"/>
                           </xsl:with-param>
                           <xsl:with-param name="p1">
                              <xsl:value-of select="@errorMsg"/>
                           </xsl:with-param>
                        </xsl:call-template>
                     </td>
                  </tr>
               </xsl:if>
            </xsl:for-each>
            <xsl:call-template name="mbeanview">
               <xsl:with-param name="objectname" select="$request.objectname"/>
            </xsl:call-template>
         </table>
      </xsl:for-each>
   </xsl:template>

   <xsl:template match="MBeanOperation">
      <html>
         <xsl:call-template name="head"/>
         <body>
            <xsl:call-template name="toprow"/>
            <xsl:call-template name="tabs">
               <xsl:with-param name="selection">mbean</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="operation"/>
            <xsl:call-template name="bottom"/>
         </body>
      </html>
   </xsl:template>
</xsl:stylesheet>

