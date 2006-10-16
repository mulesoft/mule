/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jnp;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.config.i18n.Messages;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.providers.rmi.RmiConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.services.MatchingMethodsComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * test RMI object invocations
 */
public class JnpInvocationTestCase extends FunctionalTestCase
{

    JnpConnector jnpConnector;

    protected String getConfigResources()
    {
        return null;
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.disableAdminAgent();

        // create JNP connector
        jnpConnector = new JnpConnector();
        jnpConnector.setName("jnp");
        jnpConnector.setJndiInitialFactory(MuleInitialContextFactory.class.getName());
        jnpConnector.setSecurityPolicy("rmi.policy");

        // Required if connectoring to a Remote Jndi context
        // builder.getManager().registerAgent(new RmiRegistryAgent());

        // Create a local Jndi Context
        Hashtable env = new Hashtable();
        // env.put(Context.PROVIDER_URL, "rmi://localhost:1099");
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        InitialContext ic = new InitialContext(env);
        // Bind our servcie object
        ic.bind("TestService", new MatchingMethodsComponent());

        jnpConnector.setJndiContext(ic);
        builder.getManager().registerConnector(jnpConnector);
        return builder;
    }

    public void testReverseString() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint(
            "jnp://localhost/TestService?method=reverseString", false);
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
        UMOMessage message = dispatcher.send(getTestEvent("hello", ep));
        assertNotNull(message.getPayload());
        assertEquals("olleh", message.getPayloadAsString());
    }

    public void testUpperCaseString() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint(
            "jnp://localhost/TestService?method=upperCaseString", false);
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
        UMOMessage message = dispatcher.send(getTestEvent("hello", ep));
        assertNotNull(message.getPayload());
        assertEquals("HELLO", message.getPayloadAsString());
    }

    public void testNoMethodSet() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("jnp://localhost/TestService", false);
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
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
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("jnp://localhost/TestService?method=foo", false);
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
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
        UMOEndpoint ep = new MuleEndpoint("jnp://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, StringBuffer.class.getName());
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
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
        UMOEndpoint ep = new MuleEndpoint("jnp://localhost/TestService?method=reverseString", false);
        ep.setProperty(RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES, String.class.getName());
        UMOMessageDispatcher dispatcher = jnpConnector.getDispatcher(ep);
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
