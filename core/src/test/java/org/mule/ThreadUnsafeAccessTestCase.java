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
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.transport.DefaultMessageAdapter;

public class ThreadUnsafeAccessTestCase extends AbstractThreadSafeAccessTestCase
{
    //@Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        super.configureMuleContext(contextBuilder);

        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setFailOnMessageScribbling(false);
        contextBuilder.setMuleConfiguration(config);
    }

    public void testDisable() throws InterruptedException
    {
        assertFalse(muleContext.getConfiguration().isFailOnMessageScribbling());
        ThreadSafeAccess target = new DefaultMessageAdapter(new Object());
        newThread(target, false, new boolean[]{true, true, false, true});
        newThread(target, false, new boolean[]{false});
        newThread(target, false, new boolean[]{true});
    }
}
