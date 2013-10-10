/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
