/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

@SmallTest
public class NamedStageNameSourceTest
{

    private static final String OWNER = "owner";
    private static final String NAME = "name";

    @Test
    public void getName()
    {
        final int count = 10;
        StageNameSource source = new NamedStageNameSource(OWNER, NAME);

        for (int i = 0; i < count; i++)
        {
            assertEquals(String.format("%s.%s", OWNER, NAME), source.getName());
        }
    }

    /**
     * This test verifies that if invoked concurrently,
     * the same {@link org.mule.api.processor.NamedStageNameSource}
     * doesn't break and consistently generates the same name
     */
    @Test
    public void threadSafe() throws Exception
    {
        final int count = 10;
        final StageNameSource source = new NamedStageNameSource(OWNER, NAME);
        final Set<String> names = Collections.synchronizedSet(new HashSet<String>());
        final CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++)
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    names.add(source.getName());
                    latch.countDown();
                }
            }.start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, names.size());
    }
}
