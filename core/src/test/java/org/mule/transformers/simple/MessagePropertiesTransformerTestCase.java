/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOEventContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagePropertiesTransformerTestCase extends AbstractMuleTestCase
{

    public void testOverwriteFlagEnabledByDefault() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);

        MuleMessage msg = new MuleMessage("message");
        msg.setProperty("addedProperty", "originalValue");
        UMOEventContext ctx = getTestEventContext(msg);
        MuleMessage transformed = (MuleMessage) t.transform(msg, null, ctx);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        // property values will be different
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("overwrittenValue", transformed.getProperty("addedProperty"));
    }

    public void testOverwriteFalsePreservesOriginal() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setOverwrite(false);

        MuleMessage msg = new MuleMessage("message");
        msg.setProperty("addedProperty", "originalValue");
        UMOEventContext ctx = getTestEventContext(msg);
        MuleMessage transformed = (MuleMessage) t.transform(msg, null, ctx);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("originalValue", transformed.getProperty("addedProperty"));
    }

    public void testDelete() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties(Collections.singletonList("badProperty"));

        MuleMessage msg = new MuleMessage("message");
        msg.setProperty("badProperty", "badValue");
        UMOEventContext ctx = getTestEventContext(msg);
        MuleMessage transformed = (MuleMessage) t.transform(msg, null, ctx);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());
        assertFalse(transformed.getPropertyNames().contains("badValue"));
    }

}
