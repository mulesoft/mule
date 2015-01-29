/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MessagePropertiesTransformerScopesTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testPropertyScopes() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        
        // Add properties to scope
        
        MessagePropertiesTransformer add = new MessagePropertiesTransformer();
        add.setScope(PropertyScope.INVOCATION);
        Map<String, Object> addProps = new HashMap<String, Object>();
        addProps.put("foo", "bar");
        addProps.put("foo2", "baz");
        add.setAddProperties(addProps);
        add.setMuleContext(muleContext);
        add.initialise();

        msg = (DefaultMuleMessage) add.transform(msg, (String)null);

        assertEquals("bar", msg.getInvocationProperty("foo"));
        assertNull(msg.getOutboundProperty("foo"));
        assertNull(msg.getSessionProperty("foo"));

        // Remove property from the wrong scope
        
        MessagePropertiesTransformer deleteWrongScope = new MessagePropertiesTransformer();
        deleteWrongScope.setScope(PropertyScope.OUTBOUND);
        deleteWrongScope.setDeleteProperties("foo");
        deleteWrongScope.setMuleContext(muleContext);
        deleteWrongScope.initialise();

        msg = (DefaultMuleMessage) deleteWrongScope.transform(msg, (String)null);
        assertEquals("bar", msg.getInvocationProperty("foo"));

        // Remove property from the correct scope
        
        MessagePropertiesTransformer delete = new MessagePropertiesTransformer();
        delete.setScope(PropertyScope.INVOCATION);
        delete.setDeleteProperties(Collections.singletonList("foo"));
        delete.setMuleContext(muleContext);
        delete.initialise();

        msg = (DefaultMuleMessage) delete.transform(msg, (String)null);
        assertNull(msg.getInvocationProperty("foo"));
        assertEquals("baz", msg.getInvocationProperty("foo2"));
    }
}
