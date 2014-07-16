/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.MissingResourceException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MessagesTestCase extends AbstractMuleTestCase
{

    @Test
    public void testMessageLoading() throws Exception
    {
        Message message = CoreMessages.authFailedForUser("Fred");
        assertEquals("Authentication failed for principal Fred", message.getMessage());
        assertEquals(135, message.getCode());
    }

    @Test
    public void testBadBundle()
    {
        try
        {
            InvalidMessageFactory.getInvalidMessage();
            fail("should throw resource bundle not found exception");
        }
        catch (MissingResourceException e)
        {
            // IBM JDK6: Can't find resource for bundle ...
            // Sun/IBM JDK5: Can't find bundle for base name ...
            assertTrue(e.getMessage().matches(".*Can't find.*bundle.*"));
        }
    }

    @Test
    public void testGoodBundle()
    {
        Message message = TestMessages.testMessage("one", "two", "three");
        assertEquals("Testing, Testing, one, two, three", message.getMessage());
        assertEquals(1, message.getCode());
    }
}
