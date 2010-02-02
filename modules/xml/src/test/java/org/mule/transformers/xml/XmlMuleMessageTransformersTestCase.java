/*
 * $Id:XmlMuleMessageTransformersTestCase.java 5937 2007-04-09 22:35:04Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

public class XmlMuleMessageTransformersTestCase extends AbstractMuleTestCase
{
    public void testMessageSerialization() throws Exception
    {
        ObjectToXml t1 = createObject(ObjectToXml.class);
        t1.setAcceptMuleMessage(true);

        MuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg.setEncoding("UTF-8");
        msg.setCorrelationId("1234");
        msg.setProperty("number", new Integer(1), PropertyScope.INVOCATION);
        msg.setProperty("object", new Apple(), PropertyScope.OUTBOUND);
        msg.setProperty("string", "hello", PropertyScope.OUTBOUND);

        String xml = (String) t1.transform(msg);
        assertNotNull(xml);

        XmlToObject t2 = createObject(XmlToObject.class);

        Object result = t2.transform(xml);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessage);

        msg = (MuleMessage) result;

        assertEquals("test", msg.getPayload());
        assertEquals(new Apple(), msg.getProperty("object", PropertyScope.OUTBOUND));
        assertEquals("hello", msg.getProperty("string"));
        assertEquals(new Integer(1), msg.getProperty("number", PropertyScope.INVOCATION));
        assertEquals("1234", msg.getCorrelationId());
        assertEquals("UTF-8", msg.getEncoding());

    }
}
