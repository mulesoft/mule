/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.ThreadSafeAccess;

import java.util.Map;

public class ThreadSafeAccessTestCase extends AbstractThreadSafeAccessTestCase
{
    public void testConfig()
    {
        assertTrue(ThreadSafeAccess.AccessControl.isFailOnMessageScribbling());
        assertTrue(ThreadSafeAccess.AccessControl.isAssertMessageAccess());
    }

    public void testMessage() throws InterruptedException
    {
        Map<String, Object> nullMap = null;
        basicPattern(new DefaultMuleMessage(new Object(), nullMap, muleContext));
        newCopy(new DefaultMuleMessage(new Object(), nullMap, muleContext));
        resetAccessControl(new DefaultMuleMessage(new Object(), nullMap, muleContext));
    }

    public void testEvent() throws Exception
    {
        basicPattern(dummyEvent());
        newCopy(dummyEvent());
        resetAccessControl(dummyEvent());
    }
}
