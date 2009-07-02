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
import org.mule.transport.DefaultMessageAdapter;

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
        basicPattern(new DefaultMuleMessage(new Object(), (Map)null, muleContext));
        newCopy(new DefaultMuleMessage(new Object(), (Map)null, muleContext));
        resetAccessControl(new DefaultMuleMessage(new Object(), (Map)null, muleContext));
    }

    public void testAdapter() throws InterruptedException
    {
        basicPattern(new DefaultMessageAdapter(new Object()));
        newCopy(new DefaultMessageAdapter(new Object()));
        resetAccessControl(new DefaultMessageAdapter(new Object()));
    }

    public void testEvent() throws Exception
    {
        basicPattern(dummyEvent());
        newCopy(dummyEvent());
        resetAccessControl(dummyEvent());
    }
}
