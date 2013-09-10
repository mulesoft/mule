/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
