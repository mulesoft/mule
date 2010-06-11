/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

public class GuiceAndSpringTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "guice-service-lookup-config.xml";
    }


    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        //We have to override this method since we are combining Guice and Spring in this test

        MuleContext context;
        if (getTestInfo().isDisposeManagerPerSuite() && muleContext != null)
        {
            context = muleContext;
        }
        else
        {
            MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
            List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
            builders.add(new SimpleConfigurationBuilder(getStartUpProperties()));
            builders.add(new GuiceConfigurationBuilder(new ConfigServiceModule()));
            builders.add(getBuilder());

            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            configureMuleContext(contextBuilder);
            context = muleContextFactory.createMuleContext(builders, contextBuilder);
        }
        return context;
    }


    public void testServiceLookup() throws Exception
    {

        Object service = this.getComponent("MyService2");
        assertNotNull(service);
        assertTrue(service instanceof DefaultAutoTransformService);
    }

    public void testRegisterTransformer() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send("vm://myservice2", new Orange(), null);

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof Apple);


    }

    public void testInjectForService() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send("vm://myservice3", "foo", null);

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof Banana);

    }
}
