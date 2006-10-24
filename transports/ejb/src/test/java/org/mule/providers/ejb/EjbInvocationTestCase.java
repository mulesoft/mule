/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.config.i18n.Messages;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.container.DummyEjbHomeProxy;
import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.providers.rmi.RmiConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

/**
 * test RMI object invocations
 */
public class EjbInvocationTestCase extends FunctionalTestCase
{

    private EjbConnector ejbConnector;

    protected String getConfigResources()
    {
        return null;
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.disableAdminAgent();

        // create RMI connector
        ejbConnector = new EjbConnector();
        ejbConnector.setName("ejb");
        ejbConnector.setJndiInitialFactory(MuleInitialContextFactory.class.getName());
        ejbConnector.setSecurityPolicy("rmi.policy");

        // Required if connectoring to a Remote Jndi context
        // builder.getManager().registerAgent(new RmiRegistryAgent());

        // Create a local Jndi Context
        Hashtable env = new Hashtable();
        // env.put(Context.PROVIDER_URL, "rmi://localhost:1099");
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        InitialContext ic = new InitialContext(env);
        // Bind our servcie object
        ic.bind("TestService", new DummyEjbHomeProxy());

        ejbConnector.setJndiContext(ic);
        builder.getManager().registerConnector(ejbConnector);
        return builder;
    }

    public void testReverseString() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint(
            "ejb://localhost/TestService?method=reverseString", false);
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        UMOMessage message = dispatcher.send(getTestEvent("hello", ep));
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    public void testUpperCaseString() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint(
            "ejb://localhost/TestService?method=upperCaseString", false);
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        UMOMessage message = dispatcher.send(getTestEvent("hello", ep));
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    public void testNoMethodSet() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("ejb://localhost/TestService", false);
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        try
        {
            dispatcher.send(getTestEvent("hello", ep));

        }
        catch (UMOException e)
        {
            assertTrue(e instanceof DispatchException);
            assertTrue(e.getMessage().startsWith(
                Messages.get("rmi", RmiConnector.MSG_PARAM_SERVICE_METHOD_NOT_SET)));
        }
    }

    public void testBadMethodName() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("ejb://localhost/TestService?method=foo", false);
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        try
        {
            dispatcher.send(getTestEvent("hello", ep));
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testBadMethodType() throws Exception
    {
        UMOEndpoint ep = new MuleEndpoint("ejb://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        try
        {
            dispatcher.send(getTestEvent("hello", ep));
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    public void testCorrectMethodType() throws Exception
    {
        UMOEndpoint ep = new MuleEndpoint("ejb://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, String.class.getName());
        UMOMessageDispatcher dispatcher = ejbConnector.getDispatcher(ep);
        try
        {
            dispatcher.send(getTestEvent("hello", ep));
        }
        catch (UMOException e)
        {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

}
