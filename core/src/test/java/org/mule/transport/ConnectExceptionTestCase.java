/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import org.mule.api.transport.Connectable;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.ConnectException;

import org.junit.Test;

public class ConnectExceptionTestCase extends AbstractMuleContextTestCase
{

    private static final String message = "a message";
    private static final String value = "Hello world!";

    @Test
    public void testSerializableConnectException() throws Exception
    {
        TestSerializableConnectable connectable = new TestSerializableConnectable();
        connectable.setValue(value);

        ConnectException e = new ConnectException(new Exception(message), connectable);
        e = SerializationTestUtils.testException(e, muleContext);

        assertTrue(e.getMessage().contains(message));
        Connectable failed = e.getFailed();
        assertNotNull("Connectable was not serialized", failed);
        assertTrue(failed instanceof TestSerializableConnectable);

        assertEquals(value, ((TestSerializableConnectable) failed).getValue());
    }

    @Test
    public void testNonSerializableConnectException() throws Exception
    {
        ConnectException e = new ConnectException(new Exception(message),
            new TestNotSerializableConnectable());
        e = SerializationTestUtils.testException(e, muleContext);

        assertTrue(e.getMessage().contains(message));
        Connectable failed = e.getFailed();
        assertNull(failed);
    }
}
