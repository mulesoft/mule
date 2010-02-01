/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message;

import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.MessagePropertiesContext;

public class MessagePropertiesContextTestCase extends AbstractMuleTestCase
{
    public void testPropertiesCase() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR");
        assertEquals(mpc.getProperty("foo"), "BAR");


        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);

        //Look in all scopes
        assertEquals(mpc.getProperty("doo"), "DAR");

        //Look in specific scope
        assertEquals(mpc.getProperty("doO", PropertyScope.INVOCATION), "DAR");

        //Not found using other specific scopes
        assertNull(mpc.getProperty("doo", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.OUTBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.SESSION));


    }
}
