/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class PayloadForLoggingBreaksStreamTestCase extends FunctionalTestCase
{
    private static final int invalidUTFValue = -128;

    @Test
    public void testCallGetPayloadForLogging() throws Exception
    {
        assertEquals("Set file encoding to UTF-8 for this test", StandardCharsets.UTF_8.name(), System.getProperty("file.encoding"));
        byte[] tempSourceBytes = new byte[] {invalidUTFValue};
        ByteArrayInputStream tempStream = new ByteArrayInputStream(tempSourceBytes);
        DefaultMuleMessage tempMessage = new DefaultMuleMessage(tempStream, muleContext);
        String tempString = tempMessage.getPayloadForLogging();
        assertNotNull(tempString);
        byte[] tempBytesAfterLogging = tempMessage.getPayloadAsBytes();
        assertArrayEquals(tempBytesAfterLogging, tempSourceBytes);
    }

    @Test
    public void testCallPayloadAsBytesTwice() throws Exception
    {
        assertEquals("Set file encoding to UTF-8 for this test", StandardCharsets.UTF_8.name(), System.getProperty("file.encoding"));
        byte[] tempSourceBytes = new byte[] {invalidUTFValue};
        ByteArrayInputStream tempStream = new ByteArrayInputStream(tempSourceBytes);
        DefaultMuleMessage tempMessage = new DefaultMuleMessage(tempStream, muleContext);
        byte[] tempBytes1 = tempMessage.getPayloadAsBytes();
        byte[] tempBytes2 = tempMessage.getPayloadAsBytes();
        assertArrayEquals(tempBytes2, tempBytes1);
    }

    @Test
    public void testCallPayloadAsBytesBeforeGetPayloadForLogging() throws Exception
    {
        assertEquals("Set file encoding to UTF-8 for this test", StandardCharsets.UTF_8.name(), System.getProperty("file.encoding"));
        byte[] tempSourceBytes = new byte[] {invalidUTFValue};
        ByteArrayInputStream tempStream = new ByteArrayInputStream(tempSourceBytes);
        DefaultMuleMessage tempMessage = new DefaultMuleMessage(tempStream, muleContext);
        byte[] tempBytes1 = tempMessage.getPayloadAsBytes();
        byte[] tempBytes2 = tempMessage.getPayloadAsBytes();
        assertArrayEquals(tempBytes2, tempBytes1);
        String tempString = tempMessage.getPayloadForLogging();
        assertNotNull(tempString);
        byte[] tempBytesAfterLogging = tempMessage.getPayloadAsBytes();
        assertArrayEquals(tempBytesAfterLogging, tempSourceBytes);
    }

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {"bug-login-breaks-stream.xml"};
    }
}
