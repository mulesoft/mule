/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.MuleManager;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.*;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.transformer.TransformerException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EventMetaDataProporgationTestCase extends AbstractMuleTestCase implements Callable
{
    private Apple testObjectProperty = new Apple();

    protected void setUp() throws Exception
    {
        super.setUp();
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.createStartedManager(true, null);
        UMODescriptor c1 = builder.registerComponentInstance(this, "component1",
                new MuleEndpointURI("vm://component1"),
                new MuleEndpointURI("vm://component2"));
        c1.getOutboundEndpoint().setTransformer(new DummyTransformer());

        builder.registerComponentInstance(this, "component2",
                new MuleEndpointURI("vm://component2"));
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        MuleManager.getInstance().dispose();
    }

    public void testEventMetaDataProporgation() throws UMOException
    {
        UMOSession session = MuleManager.getInstance().getModel().getComponentSession("component1");
        UMOEvent event = new MuleEvent(new MuleMessage("Test Event", null), session.getComponent().getDescriptor().getInboundEndpoint(), session, true);
        session.sendEvent(event);
    }

    public Object onCall(UMOEventContext context) throws Exception
    {
        if("component1".equals(context.getComponentDescriptor().getName())) {
            Map props = new HashMap();
            props.put("stringParam", "param1");
            props.put("objectParam",testObjectProperty);
            props.put("doubleParam", new Double(12345.6));
            props.put("integerParam", new Integer(12345));
            props.put("longParam", new Long(123456789));
            props.put("booleanParam", new Boolean(true));
            UMOMessage msg = new MuleMessage(context.getMessageAsString(), props);
            return msg;
        } else {
            assertEquals("param1", context.getProperty("stringParam"));
            assertEquals(testObjectProperty, context.getProperty("objectParam"));
            assertEquals(12345.6, 12345.6, context.getDoubleProperty("doubleParam", 0));
            assertEquals(12345, context.getIntProperty("integerParam", 0));
            assertEquals(123456789, context.getLongProperty("longParam", 0));
            assertEquals(true, context.getBooleanProperty("booleanParam", false));
        }
        return null;
    }

    private class DummyTransformer extends AbstractEventAwareTransformer
    {
        public Object transform(Object src, UMOEventContext context) throws TransformerException
        {
            assertEquals("param1", context.getProperty("stringParam"));
            assertEquals(testObjectProperty, context.getProperty("objectParam"));
            assertEquals(12345.6, 12345.6, context.getDoubleProperty("doubleParam", 0));
            assertEquals(12345, context.getIntProperty("integerParam", 0));
            assertEquals(123456789, context.getLongProperty("longParam", 0));
            assertEquals(true, context.getBooleanProperty("booleanParam", false));
            return src;
        }
    }
}
