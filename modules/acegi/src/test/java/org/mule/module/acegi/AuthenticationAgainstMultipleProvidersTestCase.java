/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.acegi;

import org.mule.tck.FunctionalTestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class AuthenticationAgainstMultipleProvidersTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "mule-multiple-providers-config.xml";
    }

    public void testProvider1() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);
        
        PostMethod postMethod = new PostMethod("http://localhost:4445");
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));
                                
        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());                  
        
        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod)); 
        
        credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod)); 
    }    
    
    public void testProvider2() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);
        
        PostMethod postMethod = new PostMethod("http://localhost:4446");
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));
                                
        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());    
        
        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod)); 
        
        credentials = new UsernamePasswordCredentials("admin", "admin");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod)); 
    }
    
    public void testMultipleProviders() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);
        
        PostMethod postMethod = new PostMethod("http://localhost:4447");
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));
                                
        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());                  
        
        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod)); 
        
        credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());
    }
    
}


