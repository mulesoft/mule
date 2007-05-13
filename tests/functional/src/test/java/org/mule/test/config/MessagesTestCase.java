/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.tck.AbstractMuleTestCase;

import java.util.MissingResourceException;

import junit.framework.Assert;

public class MessagesTestCase extends AbstractMuleTestCase
{
    public void testMessageLoading() throws Exception
    {
        Message message = CoreMessages.authFailedForUser("Fred");
        Assert.assertEquals("Authentication failed for principal Fred", message.getMessage());
        Assert.assertEquals(135, message.getCode());
    }

    public void testBadBundle()
    {
        try
        {
            InvalidMessageFactory.getInvalidMessage();
            Assert.fail("should throw resource bundle not found exception");
        }
        catch (MissingResourceException e)
        {
            Assert.assertTrue(e.getMessage().startsWith("Can't find bundle"));
        }
    }

    public void testGoodBundle()
    {
        Message message = TestMessages.testMessage("one", "two", "three");
        Assert.assertEquals("Testing, Testing, one, two, three", message.getMessage());
        Assert.assertEquals(1, message.getCode());
    }
}
