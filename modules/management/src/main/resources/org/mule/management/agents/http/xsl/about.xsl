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
   <xsl:output method="html" indent="yes" encoding="ISO-8859-1"/>
   <xsl:include href="common.xsl"/>
   <xsl:include href="xalan-ext.xsl"/>

   <xsl:param name="html.stylesheet">stylesheet.css</xsl:param>
   <xsl:param name="html.stylesheet.type">text/css</xsl:param>
   <xsl:param name="head.title">about.title</xsl:param>

   <!-- Main template -->
   <xsl:template match="/MBean">
      <html>
         <xsl:call-template name="head"/>
         <body>
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
               <tr width="100%">
                  <td>
                     <xsl:call-template name="toprow"/>
                     <xsl:call-template name="tabs">
                        <xsl:with-param name="selection">about</xsl:with-param>
                     </xsl:call-template>
                     <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                           <td colspan="2" class="page_title">
                              <xsl:call-template name="str">
                                 <xsl:with-param name="id">about.main.title</xsl:with-param>
                                 <xsl:with-param name="p0">
                                    <xsl:call-template name="productName"/>
                                 </xsl:with-param>
                              </xsl:call-template>
                           </td>
                        </tr>
                        <tr class="about">
                           <td colspan="2">
                              <h1 align="center" class="about">
                                 <xsl:variable name="product.url">
                                    <xsl:call-template name="productUrl"/>
                                 </xsl:variable>
                                 <a href="{$product.url}">
                                 <xsl:call-template name="str">
                                       <xsl:with-param name="id">about.main.mule.version</xsl:with-param>
                                    <xsl:with-param name="p0">
                                          <xsl:call-template name="productDescription"/>
                                    </xsl:with-param>
                                       <xsl:with-param name="p1">
                                          <xsl:call-template name="productVersion"/>
                                       </xsl:with-param>
                                    </xsl:call-template>
                                 </a>
                                 <br/>
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.mule.title</xsl:with-param>
                                 </xsl:call-template>
                                 <br/>
                                 <xsl:variable name="vendor.url">
                                    <xsl:call-template name="vendorUrl"/>
                                 </xsl:variable>
                                 <a href="{$vendor.url}">
                                    <xsl:call-template name="vendorName"/>
                                 </a>
                              </h1>
                           </td>
                        </tr>
                        <tr class="about">
                           <td colspan="2">
                              <h2 align="center" class="about">
                                 <xsl:variable name="str.url">
                                    <xsl:call-template name="str">
                                       <xsl:with-param name="id">about.main.mx4j.url</xsl:with-param>
                                    </xsl:call-template>
                                 </xsl:variable>
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.mx4j.title</xsl:with-param>
                                    <xsl:with-param name="p0">
                                       <a href="{$str.url}">MX4J</a>
                                    </xsl:with-param>
                                 </xsl:call-template>
                              </h2>
                           </td>
                        </tr>
                        <tr class="about">
                           <td colspan="2">
                              <h2 align="center" class="about">
                                 <xsl:variable name="str.url">
                                    <xsl:call-template name="str">
                                       <xsl:with-param name="id">about.main.jmx.url</xsl:with-param>
                                    </xsl:call-template>
                                 </xsl:variable>
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.jmx.title</xsl:with-param>
                                    <xsl:with-param name="p0">
                                       <a href="{$str.url}">JMX</a>
                                    </xsl:with-param>
                                 </xsl:call-template>
                              </h2>
                           </td>
                        </tr>
                        <tr class="about">
                           <td>
                              <h3 align="right" class="about">
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.implementation</xsl:with-param>
                                 </xsl:call-template>
                              </h3>
                           </td>
                           <td>
                              <h3 align="left" class="about">
                                 <xsl:value-of select="./Attribute[@name='ImplementationName']/@value"/>
                              </h3>
                           </td>
                        </tr>
                        <tr class="about">
                           <td>
                              <h3 align="right" class="about">
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.implementationversion</xsl:with-param>
                                 </xsl:call-template>
                              </h3>
                           </td>
                           <td>
                              <h3 align="left" class="about">
                                 <xsl:value-of select="./Attribute[@name='ImplementationVersion']/@value"/>
                              </h3>
                           </td>
                        </tr>
                        <tr class="about">
                           <td>
                              <h3 align="right" class="about">
                                 <xsl:call-template name="str">
                                    <xsl:with-param name="id">about.main.serverid</xsl:with-param>
                                 </xsl:call-template>
                              </h3>
                           </td>
                           <td>
                              <h3 align="left" class="about">
                                 <xsl:value-of select="./Attribute[@name='MBeanServerId']/@value"/>
                              </h3>
                           </td>
                        </tr>
                     </table>
                     <xsl:call-template name="bottom"/>
                  </td>
               </tr>
            </table>
         </body>
      </html>
   </xsl:template>
</xsl:stylesheet>

