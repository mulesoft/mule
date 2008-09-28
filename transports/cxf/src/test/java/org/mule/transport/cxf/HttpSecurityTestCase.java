/*
 * $Id: HttpSecurityTestCase.java 11449 2008-03-20 12:27:50Z dandiep $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cxf;

import org.mule.tck.FunctionalTestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;


public class HttpSecurityTestCase extends FunctionalTestCase 
{
    
    private static String soapRequest = 
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:unk=\"http://unknown.namespace/\">" +
           "<soapenv:Header/>" +
           "<soapenv:Body>" +
              "<unk:echo>" +         
                 "<arg0>asdf</arg0>" +
              "</unk:echo>" +
           "</soapenv:Body>" +
        "</soapenv:Envelope>";
    
    public void testBasicAuth() throws Exception
    {
        HttpClient client = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin", "admin");
        client.getState().setCredentials(AuthScope.ANY, credentials);

        PostMethod method = new PostMethod("https://localhost:60443/services/Echo");
        method.setDoAuthentication(true);
        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/plain", "UTF-8");
        method.setRequestEntity(requestEntity);
        
        int result = client.executeMethod(method);

        assertEquals(200, result);
        System.out.println(method.getResponseBodyAsString());

        credentials = new UsernamePasswordCredentials("admin", "adminasd");
        client.getState().setCredentials(AuthScope.ANY, credentials);
        result = client.executeMethod(method);

        assertEquals(401, result);
    }

    @Override
    protected String getConfigResources()
    {
        return "http-security-conf.xml";
    }   
    
}   
