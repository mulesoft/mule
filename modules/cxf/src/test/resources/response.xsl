<!--
 $Id: about.xsl 8077 2007-08-27 20:15:25Z aperepel $
 
 Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 
 The software in this package is published under the terms of the CPAL v1.0
 license, a copy of which has been included with this distribution in the
 LICENSE.txt file.
 -->
 <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xsl:version="2.0">
  <soap:Body>
  <ns1:echoResponse xmlns:ns1="http://simple.component.mule.org/">
  <ns1:return>Hello Transformed!</ns1:return>
  </ns1:echoResponse>
  </soap:Body>
</soap:Envelope>