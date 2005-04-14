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
 *
 */

package org.mule.test.mule;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.ResponseOutputStream;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformers.DefaultTransformer;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.TransformerException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleEventTestCase extends AbstractMuleTestCase
{
    public void testEventInitialise() throws Exception
    {
        String data = "Test Data";
        MuleDescriptor descriptor = getTestDescriptor("orange", Orange.class.getName());

        MuleEvent event = (MuleEvent) getTestEvent(data, descriptor);

        assertEquals("Event data should equal " + data, data, event.getMessage().getPayload());
        assertEquals("Event data should equal " + data, data, event.getMessageAsString());
        assertEquals("Event data should equal " + data, data, event.getTransformedMessage());
        assertEquals("Event data should be a byte array 9 bytes in length", 9, event.getTransformedMessageAsBytes().length);

        assertEquals("Event data should be a byte array 9 bytes in length", 9, event.getMessageAsBytes().length);
        assertEquals("Event data should equal " + data, data, event.getSource());
        
        assertEquals("MuleBeanPropertiesRule", event.getProperty("MuleBeanPropertiesRule", "MuleBeanPropertiesRule"));
        event.setProperty("Test", "Test1");

        assertNotNull(event.getProperties());
        assertTrue(event.getProperties().entrySet().iterator().hasNext());
        assertEquals("bla2", event.getProperty("bla2", "bla2"));
        assertEquals("Test1", event.getProperty("Test"));
        assertEquals("Test1", event.getProperty("Test", "bla2"));
        assertNotNull(event.getId());
    }

    public void testEventTransformer() throws Exception
    {
        String data = "Test Data";
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setTransformer(new TestEventTransformer());
        UMOEvent event = getTestEvent(data, endpoint);

        assertEquals("Event data should equal " + data, data, event.getMessage().getPayload());
        assertEquals("Event data should equal " + data, data, event.getMessageAsString());
        assertEquals("Event data should equal 'Transformed Test Data'", "Transformed Test Data", event.getTransformedMessage());
        assertEquals("Event data should be a byte array 28 bytes in length", 21, event.getTransformedMessageAsBytes().length);
    }

    public void testEventRewrite() throws Exception
    {
        String data = "Test Data";
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setTransformer(new TestEventTransformer());
        MuleEvent event = new MuleEvent(new MuleMessage(data, null),
                endpoint,
                getTestSession(getTestComponent(getTestDescriptor("apple", Apple.class.getName()))),
                true, new ResponseOutputStream(System.out));

        assertNotNull(event.getId());
        assertNotNull(event.getSession());
        assertNotNull(event.getEndpoint());
        assertNotNull(event.getOutputStream());
        assertNotNull(event.getMessage());
        assertEquals(data, event.getMessageAsString());

        UMOEvent event2 = new MuleEvent(new MuleMessage("New Data", null), event);
        assertNotNull(event2.getId());
        assertEquals(event.getId(), event2.getId());
        assertNotNull(event2.getSession());
        assertNotNull(event2.getEndpoint());
        assertNotNull(event2.getOutputStream());
        assertNotNull(event2.getMessage());
        assertEquals("New Data", event2.getMessageAsString());

    }

    private class TestEventTransformer extends DefaultTransformer
    {
        /* (non-Javadoc)
        * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
        */
        public Object doTransform(Object src) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

}
