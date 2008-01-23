/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.AbstractEventAwareTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;

public class EventMetaDataPropagationTestCase extends FunctionalTestCase implements Callable
{
    private Apple testObjectProperty = new Apple();

    protected String getConfigResources()
    {
        return "org/mule/test/integration/event-metadata-propagation-config.xml";
    }

    public void testEventMetaDataPropagation() throws MuleException
    {
        Component component = muleContext.getRegistry().lookupComponent("component1");
        OutboundRouter outboundRouter = (OutboundRouter) component.getOutboundRouter().getRouters().get(0);
        Endpoint endpoint = (Endpoint) outboundRouter.getEndpoints().get(0);
        List transformers = new ArrayList();
        transformers.add(DummyTransformer.class);
        endpoint.setTransformers(transformers);
        
        MuleSession session = new DefaultMuleSession(component);

        MuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage("Test MuleEvent"), (ImmutableEndpoint)component
                .getInboundRouter().getEndpoints().get(0), session, true);
        session.sendEvent(event);
    }

    public Object onCall(MuleEventContext context) throws Exception
    {
        if ("component1".equals(context.getComponent().getName()))
        {
            Map props = new HashMap();
            props.put("stringParam", "param1");
            props.put("objectParam", testObjectProperty);
            props.put("doubleParam", new Double(12345.6));
            props.put("integerParam", new Integer(12345));
            props.put("longParam", new Long(123456789));
            props.put("booleanParam", Boolean.TRUE);
            MuleMessage msg = new DefaultMuleMessage(context.getMessageAsString(), props);
            msg.addAttachment("test1", new DataHandler(new DataSource()
            {
                public InputStream getInputStream() throws IOException
                {
                    return null;
                }

                public OutputStream getOutputStream() throws IOException
                {
                    return null;
                }

                public String getContentType()
                {
                    return "text/plain";
                }

                public String getName()
                {
                    return "test1";
                }
            }));
            return msg;
        }
        else
        {
            MuleMessage msg = context.getMessage();
            assertEquals("param1", msg.getProperty("stringParam"));
            assertEquals(testObjectProperty, msg.getProperty("objectParam"));
            assertEquals(12345.6, 12345.6, msg.getDoubleProperty("doubleParam", 0));
            assertEquals(12345, msg.getIntProperty("integerParam", 0));
            assertEquals(123456789, msg.getLongProperty("longParam", 0));
            assertEquals(true, msg.getBooleanProperty("booleanParam", false));
            assertNotNull(msg.getAttachment("test1"));
        }
        return null;
    }

    private class DummyTransformer extends AbstractEventAwareTransformer
    {
        public Object transform(Object src, String encoding, MuleEventContext context)
            throws TransformerException
        {
            MuleMessage msg = context.getMessage();
            assertEquals("param1", msg.getProperty("stringParam"));
            assertEquals(testObjectProperty, msg.getProperty("objectParam"));
            assertEquals(12345.6, 12345.6, msg.getDoubleProperty("doubleParam", 0));
            assertEquals(12345, msg.getIntProperty("integerParam", 0));
            assertEquals(123456789, msg.getLongProperty("longParam", 0));
            assertEquals(true, msg.getBooleanProperty("booleanParam", false));
            return src;
        }
    }
}
