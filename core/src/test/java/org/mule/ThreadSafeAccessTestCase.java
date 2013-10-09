/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertTrue;

import org.mule.api.ThreadSafeAccess;

import java.util.Map;

import org.junit.Test;

public class ThreadSafeAccessTestCase extends AbstractThreadSafeAccessTestCase
{
    @Test
    public void testConfig()
    {
        assertTrue(ThreadSafeAccess.AccessControl.isFailOnMessageScribbling());
        assertTrue(ThreadSafeAccess.AccessControl.isAssertMessageAccess());
    }

    @Test
    public void testMessage() throws InterruptedException
    {
        Map<String, Object> nullMap = null;
        basicPattern(new DefaultMuleMessage(new Object(), nullMap, muleContext));
        newCopy(new DefaultMuleMessage(new Object(), nullMap, muleContext));
        resetAccessControl(new DefaultMuleMessage(new Object(), nullMap, muleContext));
    }

    @Test
    public void testEvent() throws Exception
    {
        // TODO Broken because we read a message property as part of event constructor (BL-677)
        //basicPattern(dummyEvent());
        //newCopy(dummyEvent());
        resetAccessControl(dummyEvent());
    }
}
