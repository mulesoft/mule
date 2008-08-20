/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.cxf;

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

    @Override
    protected String getConfigResources()
    {
        return "http-security-conf.xml";
    }   

    public void testBasicAuth() throws Exception 
    {
//        //BasicConfigurator.configure(); 
//        MuleServer muleServer = new MuleServer(""); 
//        muleServer.start(false, true); 

        HttpClient client = new HttpClient(); 
        Credentials credentials = new UsernamePasswordCredentials("admin", "admin"); 
        AuthScope authScope = new AuthScope("localhost", 63081, "mule-realm"); 
        client.getState().setCredentials(authScope, credentials); 

        PostMethod method = new PostMethod("http://localhost:63081/services/Echo"); 
        method.setDoAuthentication(true);         
        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/plain", "UTF-8");                 
        method.setRequestEntity(requestEntity); 

        int result = client.executeMethod(method); 
        assertEquals(200, result); 

//        credentials = new UsernamePasswordCredentials("admin", "adminasd"); 
//        client.getState().setCredentials(authScope, credentials); 
//        result = client.executeMethod(method); 
//        assertEquals(401, result); 
    }

}


