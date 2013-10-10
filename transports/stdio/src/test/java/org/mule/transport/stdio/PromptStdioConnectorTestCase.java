/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.stdio;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PromptStdioConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public String getTestEndpointURI()
    {
        return "stdio://System.out";
    }

    @Override
    public Connector createConnector() throws Exception
    {
        Connector cnn = new PromptStdioConnector(muleContext);
        cnn.setName("TestStdio");
        return cnn;
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

    @Test
    public void testContextClassLoaderResourceLookup() throws InitialisationException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        PromptStdioConnector connector = new PromptStdioConnector(muleContext);
        connector.setResourceBundle("dummy-messages");
        connector.setPromptMessageCode("1");
        connector.setOutputMessageCode("2");
        try
        {
            Thread.currentThread().setContextClassLoader(new ContextClassLoaderTestClassLoader());
            connector.doInitialise();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        assertEquals("Test ContextClassLoader Prompt Message", connector.getPromptMessage());
        assertEquals("Test ContextClassLoader Output Message", connector.getOutputMessage());
    }

    private class ContextClassLoaderTestClassLoader extends ClassLoader
    {
        public ContextClassLoaderTestClassLoader()
        {
            super();
        }
        
        @Override
        public InputStream getResourceAsStream(String name)
        {
            String messages = "1=Test ContextClassLoader Prompt Message\n2=Test ContextClassLoader Output Message";
            return new ByteArrayInputStream(messages.getBytes());
        }
    }
}
