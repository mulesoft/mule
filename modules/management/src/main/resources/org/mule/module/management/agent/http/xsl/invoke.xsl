<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <!-- Overall parameters -->
   <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
   <xsl:param name="html.stylesheet.type">text/css</xsl:param>
   <xsl:param name="head.title">invoke.title</xsl:param>

   <!-- Request parameters -->
   <xsl:param name="request.objectname"/>
   <xsl:param name="request.method"/>

   <xsl:include href="common.xsl"/>
   <xsl:include href="mbean_attributes.xsl"/>

   <!-- Operation invoke -->
   <xsl:template name="operation">
      <xsl:for-each select="Operation">
         <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
               <td width="100%" class="page_title">
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">invoke.operation.title</xsl:with-param>
                     <xsl:with-param name="p0">
                        <xsl:value-of select="$request.method"/>
                     </xsl:with-param>
                     <xsl:with-param name="p1">
                        <xsl:value-of select="$request.objectname"/>
                     </xsl:with-param>
                  </xsl:call-template>
               </td>
            </tr>
            <tr class="darkline">
               <td>
                  <div class="tableheader">
                     <xsl:if test="@result='success'">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.success</xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="@result='error'">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.error</xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                  </div>
               </td>
            </tr>
            <tr class="clearline">
               <td class="serverbydomain_row">
                  <xsl:if test="@result='success'">
                     <xsl:if test="not (@return='')">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.success.result</xsl:with-param>
                           <xsl:with-param name="p0">
                              <xsl:call-template name="renderobject">
                                 <xsl:with-param name="objectclass" select="@returnclass"/>
                                 <xsl:with-param name="objectvalue" select="@return"/>
                              </xsl:call-template>
                           </xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="@return=''">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">invoke.operation.success.noresult</xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                  </xsl:if>
                  <xsl:if test="@result='error'">
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
                  </xsl:if>
               </td>
            </tr>
            <xsl:call-template name="mbeanview">
               <xsl:with-param name="objectname" select="$request.objectname"/>
            </xsl:call-template>
         </table>
      </xsl:for-each>
   </xsl:template>

   <!-- Main template -->
   <xsl:template match="MBeanOperation" name="main">
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

