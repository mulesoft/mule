<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="html" indent="yes" encoding="UTF-8"/>

   <!-- Overall parameters -->
   <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
   <xsl:param name="html.stylesheet.type">text/css</xsl:param>
   <xsl:param name="head.title">monitor_create.title</xsl:param>

   <!-- Request parameters -->
   <xsl:param name="request.objectname"/>
   <xsl:param name="request.class"/>

   <xsl:include href="common.xsl"/>

   <xsl:template name="operation">
      <xsl:for-each select="Operation">
         <xsl:variable name="monitor_type">
            <xsl:if test="$request.class='javax.management.monitor.StringMonitor'">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">monitor_create.operation.stringmonitor</xsl:with-param>
               </xsl:call-template>
            </xsl:if>
            <xsl:if test="$request.class='javax.management.monitor.GaugeMonitor'">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">monitor_create.operation.gaugemonitor</xsl:with-param>
               </xsl:call-template>
            </xsl:if>
            <xsl:if test="$request.class='javax.management.monitor.CounterMonitor'">
               <xsl:call-template name="str">
                  <xsl:with-param name="id">monitor_create.operation.countermonitor</xsl:with-param>
               </xsl:call-template>
            </xsl:if>
         </xsl:variable>
         <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
               <td width="100%" class="page_title">
                  <xsl:value-of select="$monitor_type"/>
                  <xsl:call-template name="str">
                     <xsl:with-param name="id">monitor_create.operation.title</xsl:with-param>
                     <xsl:with-param name="p0">
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
                           <xsl:with-param name="id">monitor_create.operation.success</xsl:with-param>
                           <xsl:with-param name="p0">
                              <xsl:value-of select="$monitor_type"/>
                           </xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                     <xsl:if test="@result='error'">
                        <xsl:call-template name="str">
                           <xsl:with-param name="id">monitor_create.operation.error</xsl:with-param>
                           <xsl:with-param name="p0">
                              <xsl:value-of select="$monitor_type"/>
                           </xsl:with-param>
                           <xsl:with-param name="p1">
                              <xsl:value-of select="@errorMsg"/>
                           </xsl:with-param>
                        </xsl:call-template>
                     </xsl:if>
                  </div>
               </td>
            </tr>
            <xsl:if test="@result='success'">
               <xsl:call-template name="mbeanview">
                  <xsl:with-param name="objectname">
                     <xsl:value-of select="$request.objectname"/>
                  </xsl:with-param>
                  <xsl:with-param name="text">monitor_create.operation.mbeanview</xsl:with-param>
               </xsl:call-template>
            </xsl:if>
            <xsl:if test="@result='error'">
               <xsl:call-template name="serverview"/>
            </xsl:if>
         </table>
      </xsl:for-each>
   </xsl:template>

   <xsl:template match="MBeanOperation">
      <html>
         <xsl:call-template name="head"/>
         <body>
            <xsl:call-template name="toprow"/>
            <xsl:call-template name="tabs">
               <xsl:with-param name="selection">monitor</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="operation"/>
            <xsl:call-template name="bottom"/>
         </body>
      </html>
   </xsl:template>
</xsl:stylesheet>

