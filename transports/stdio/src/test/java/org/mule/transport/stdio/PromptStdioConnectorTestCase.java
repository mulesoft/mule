/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.stdio;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class PromptStdioConnectorTestCase extends AbstractConnectorTestCase
{

    private CountDownLatch latch;

    public String getTestEndpointURI()
    {
        return "stdio://System.out";
    }

    public Connector createConnector() throws Exception
    {
        Connector cnn = new PromptStdioConnector();
        cnn.setName("TestStdio");
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }

    public void testContextClassLoaderResourceLookup() throws InitialisationException
    {
        ClassLoader testClassLoader = Thread.currentThread().getContextClassLoader();
        PromptStdioConnector connector = new PromptStdioConnector();
        connector.setMuleContext(muleContext);
        connector.setResourceBundle("dummy-messages");
        connector.setPromptMessageCode("1");
        connector.setOutputMessageCode("2");
        Thread.currentThread().setContextClassLoader(new ContextClassLoaderTestClassLoader());
        connector.doInitialise();
        Thread.currentThread().setContextClassLoader(testClassLoader);

        assertEquals("Test ContextClassLoader Prompt Message", connector.getPromptMessage());
        assertEquals("Test ContextClassLoader Output Message", connector.getOutputMessage());
    }

    private class ContextClassLoaderTestClassLoader extends ClassLoader
    {

        @Override
        public InputStream getResourceAsStream(String name)
        {
            return new StringBufferInputStream(
                "1=Test ContextClassLoader Prompt Message\n2=Test ContextClassLoader Output Message");
        }
    }

}
