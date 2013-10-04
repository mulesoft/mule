/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.rmi;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.util.concurrent.Latch;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This is a UNIT TEST case, not a functional test case.
 *
 * @author Yuen-Chi Lian
 */
public class RmiMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    private static Log LOGGER = LogFactory.getLog(RmiMessageReceiverTestCase.class);

    private RmiConnector connector = null;
    private RmiMessageReceiver messageReceiver = null;
    private Registry rmiRegistry = null;

    @Override
    protected void doSetUp() throws Exception
    {
        registerRmi();

        connector = new RmiConnector(muleContext);
        connector.setName("TestConnector:" + this.getClass());
        connector.setSecurityPolicy(ClassLoader.getSystemResource("rmi.policy").getPath());
        connector.setJndiInitialFactory("com.sun.jndi.rmi.registry.RegistryContextFactory");
        connector.setJndiProviderUrl("rmi://localhost:11099");

        muleContext.getRegistry().registerConnector(connector);

        super.doSetUp();
    }

    private void registerRmi() throws Exception
    {
        if (null == rmiRegistry)
        {
            rmiRegistry = LocateRegistry.createRegistry(11099);
            Naming.rebind("//localhost:11099/TestMatchingMethodsComponent",
                new SerializedMatchingMethodsComponent());

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");

            Context context = new InitialContext(env);
            SerializedMatchingMethodsComponent obj =
                (SerializedMatchingMethodsComponent) context.lookup("rmi://localhost:11099/TestMatchingMethodsComponent");

            if (obj == null)
            {
                throw new RuntimeException("Could not start RMI properly");
            }
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        try
        {
            messageReceiver.disconnect();   // the message receiver is disposed by its connector

            connector.disconnect();
            connector.dispose();

            UnicastRemoteObject.unexportObject(rmiRegistry, true);
        }
        catch (Exception e)
        {
            LOGGER.warn(e.toString(), e);
        }

        super.doTearDown();
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        if (this.endpoint == null)
        {
            EndpointBuilder builder = new EndpointURIEndpointBuilder(
                "rmi://localhost:11099/TestMatchingMethodsComponent?method=reverseString", muleContext);

            if (connector == null)
            {
                throw new InitialisationException(
                    MessageFactory.createStaticMessage("Connector has not been initialized."), null);
            }
            builder.setConnector(connector);

            Map<Object, Object> properties = new HashMap<Object, Object>();
            properties.put("methodArgumentTypes", "java.lang.String");
            properties.put("methodArgumentsList", Arrays.asList(new String[]{"test"}));

            builder.setProperties(properties);
            endpoint = muleContext.getEndpointFactory().getInboundEndpoint(builder);

            return endpoint;
        }
        return this.endpoint;
    }

    @Override
    public RmiMessageReceiver getMessageReceiver() throws Exception
    {
        if (messageReceiver == null)
        {
            messageReceiver = new RmiMessageReceiver(this.connector, getTestService(),
                this.getEndpoint(), 5000)
            {
                @Override
                public void poll()
                {
                    super.poll();
                    RmiMessageReceiverTestCase.this.callbackCalled.countDown();
                }

            };
            messageReceiver.initialise();
            messageReceiver.setListener(getSensingNullMessageProcessor());
        }
        return messageReceiver;
    }

    @Test
    public void testReceive() throws Exception
    {
        RmiMessageReceiver rmiMessageReceiver = getMessageReceiver();

        // Before connect(), let's do some assertion
        assertNull(rmiMessageReceiver.invokeMethod);
        rmiMessageReceiver.connect();

        // Make sure that the transport could find the method
        assertNotNull(rmiMessageReceiver.invokeMethod);

        // Poll once
        callbackCalled = new Latch();
        rmiMessageReceiver.poll();
        assertTrue(callbackCalled.await(1000, TimeUnit.MILLISECONDS));
    }
}
