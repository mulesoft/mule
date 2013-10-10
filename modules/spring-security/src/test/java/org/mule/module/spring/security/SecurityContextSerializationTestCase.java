/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class SecurityContextSerializationTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "security-context-serialization-test-case.xml";
    }

    @Test
    public void testAuthorizedInAnotherFlow() throws Exception
    {
        doRequest(null, "localhost", "ross", "ross", getUrl(), false, 200);
    }

    protected String getUrl()
    {
        return String.format("http://localhost:%s/authorize",httpPort.getNumber());
    }

    private void doRequest(String realm,
                           String host,
                           String user,
                           String pass,
                           String url,
                           boolean handshake,
                           int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        GetMethod get = new GetMethod(url);
        get.setDoAuthentication(handshake);

        try
        {
            int status = client.executeMethod(get);
            if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
            {
                // doAuthentication = true means that if the request returns 401, 
                // the HttpClient will resend the request with credentials
                status = client.executeMethod(get);
                if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
                {
                    // doAuthentication = true means that if the request returns 401, 
                    // the HttpClient will resend the request with credentials
                    status = client.executeMethod(get);
                }
            }
            assertEquals(result, status);
        }
        finally
        {
            get.releaseConnection();
        }
    }

    public static class AddNotSerializableProperty implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            event.getMessage().setInvocationProperty("notSerializableProperty",new Object());
            return event;
        }
    }

}
