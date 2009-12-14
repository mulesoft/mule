/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagePropertiesTransformerScopesTestCase extends AbstractMuleTestCase
{
    public void testPropertyScopes() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        
        // Add properties to scope
        
        MessagePropertiesTransformer add = new MessagePropertiesTransformer();
        add.setScope(PropertyScope.INVOCATION);
        Map addProps = new HashMap();
        addProps.put("foo", "bar");
        addProps.put("foo2", "baz");
        add.setAddProperties(addProps);
        add.setMuleContext(muleContext);

        msg = (DefaultMuleMessage) add.transform(msg, null);

        assertEquals("bar", msg.getProperty("foo", PropertyScope.INVOCATION));
        assertNull(msg.getProperty("foo", PropertyScope.OUTBOUND));
        assertNull(msg.getProperty("foo", PropertyScope.SESSION));

        // Remove property from the wrong scope
        
        MessagePropertiesTransformer deleteWrongScope = new MessagePropertiesTransformer();
        deleteWrongScope.setScope(PropertyScope.OUTBOUND);
        deleteWrongScope.setDeleteProperties(Collections.singletonList("foo"));
        deleteWrongScope.setMuleContext(muleContext);

        msg = (DefaultMuleMessage) deleteWrongScope.transform(msg, null);
        assertEquals("bar", msg.getProperty("foo", PropertyScope.INVOCATION));

        // Remove property from the correct scope
        
        MessagePropertiesTransformer delete = new MessagePropertiesTransformer();
        add.setScope(PropertyScope.INVOCATION);
        delete.setDeleteProperties(Collections.singletonList("foo"));
        delete.setMuleContext(muleContext);

        msg = (DefaultMuleMessage) delete.transform(msg, null);
        assertNull(msg.getProperty("foo", PropertyScope.INVOCATION));
        assertEquals("baz", msg.getProperty("foo2", PropertyScope.INVOCATION));
    }
}
