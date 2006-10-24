/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.components.simple.NullComponent;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.message.ExceptionMessage;
import org.mule.providers.vm.VMConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.TransformerException;

public class ExceptionListenerTestCase extends FunctionalTestCase
{

    public ExceptionListenerTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return null;
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();

        // Create VM connector that queues events
        VMConnector cnn = new VMConnector();
        cnn.setName("vmCnn");
        cnn.setQueueEvents(true);
        builder.getManager().registerConnector(cnn);
        DefaultComponentExceptionStrategy es = new DefaultComponentExceptionStrategy();
        es.addEndpoint(new MuleEndpoint("vm://error.queue", false));
        builder.getManager().getModel().setExceptionListener(es);
        UMOEndpoint ep = new MuleEndpoint("vm://component1", true);
        ep.setTransformer(new AbstractTransformer()
        {

            protected Object doTransform(Object src, String encoding) throws TransformerException
            {
                throw new TransformerException(this, new Exception("My transformer is broken"));
            }
        });

        builder.registerComponent(Object.class.getName(), "component1", ep, new MuleEndpoint(
            "vm://component1.out", false), null);

        builder.registerComponent(NullComponent.class.getName(), "component2", "vm://component2",
            "vm://component2.out", null);
        return builder;
    }

    public void testExceptionStrategyFromComponent() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.receive("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://component2", "test", null);

        message = client.receive("vm://component2.out", 2000);
        assertNull(message);

        message = client.receive("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }

    public void testExceptionStrategyForTransformerException() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.receive("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://component1", "test", null);

        message = client.receive("vm://component1.out", 2000);
        assertNull(message);

        message = client.receive("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }

    public void testExceptionStrategyForTransformerExceptionAsync() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.receive("vm://error.queue", 2000);
        assertNull(message);

        client.dispatch("vm://component1", "test", null);

        message = client.receive("vm://component1.out", 2000);
        assertNull(message);

        message = client.receive("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }
}
