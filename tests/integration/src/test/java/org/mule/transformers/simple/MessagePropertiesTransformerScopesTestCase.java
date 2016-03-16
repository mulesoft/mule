/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mule.PropertyScope.OUTBOUND;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessagePropertiesTransformerScopesTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testPropertyScopes() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        
        // Add properties to scope
        
        MessagePropertiesTransformer add = new MessagePropertiesTransformer();
        Map<String, Object> addProps = new HashMap<String, Object>();
        addProps.put("foo", "bar");
        addProps.put("foo2", "baz");
        add.setAddProperties(addProps);
        add.setMuleContext(muleContext);
        add.initialise();

        msg = (DefaultMuleMessage) add.transform(msg, (String)null);

        assertEquals("bar", msg.getOutboundProperty("foo"));
        assertEquals("bar", msg.getProperty("foo", OUTBOUND));

        // Remove property from scope
        
        MessagePropertiesTransformer delete = new MessagePropertiesTransformer();
        delete.setDeleteProperties(Collections.singletonList("foo"));
        delete.setMuleContext(muleContext);
        delete.initialise();

        msg = (DefaultMuleMessage) delete.transform(msg, (String)null);
        assertNull(msg.getOutboundProperty("foo"));
        assertEquals("baz", msg.getOutboundProperty("foo2"));
    }
}
