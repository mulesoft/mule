<?xml version="1.0"?>
<!--
 $Id$
 
 Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 
 The software in this package is published under the terms of the CPAL v1.0
 license, a copy of which has been included with this distribution in the
 LICENSE.txt file.
 -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
   <xsl:include href="common.xsl"/>
   
   <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
   <xsl:param name="html.stylesheet.type">text/css</xsl:param>
   <xsl:param name="head.title">statistics.title</xsl:param>

   <xsl:template name="operation">
      <xsl:for-each select="Operation">
         <xsl:if test="@result='success'">
            <xsl:value-of select="@return" disable-output-escaping="yes"/>
         </xsl:if>
         <xsl:if test="@result='error'">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
               <tr class="darkline">
                  <td>
                     <div class="tableheader">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.error</xsl:with-param>
                        </xsl:call-template>
                     </div>
                  </td>
               </tr>
               <tr class="clearline">
                  <td class="serverbydomain_row">
                     <xsl:if test="not (@return='')">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.error.message</xsl:with-param>
                           <xsl:with-param name="p0">
                              <xsl:value-of select="@errorMsg"/>
                           </xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="@return=''">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.error.noresult</xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                  </td>
               </tr>
               <xsl:call-template name="serverview"/>
            </table>
         </xsl:if>
      </xsl:for-each>
   </xsl:template>

   <!-- Main template -->
   <xsl:template match="MBeanOperation">
      <html>
      <xsl:call-template name="head"/>
         <body>
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
               <tr width="100%">
                  <td>
                     <xsl:call-template name="toprow"/>
                     <xsl:call-template name="tabs">
                        <xsl:with-param name="selection">statistics</xsl:with-param>
                     </xsl:call-template>
                     <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                           <td class="page_title">
                              <xsl:call-template name="str">
                                 <xsl:with-param name="id">statistics.subtitle</xsl:with-param>
                              </xsl:call-template>
                              <xsl:value-of select="substring-before(Operation/@objectname,':')"/>
                           </td>
                        </tr>
                     </table>
                     <xsl:call-template name="operation"/>
                     <xsl:call-template name="bottom"/>
                  </td>
               </tr>
            </table>
         </body>
      </html>
   </xsl:template>
</xsl:stylesheet>

