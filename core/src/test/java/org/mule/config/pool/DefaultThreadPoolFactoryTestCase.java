/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.pool;

import org.mule.api.config.ThreadingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DefaultThreadPoolFactoryTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testDefaults() throws Exception
    {
        final ThreadingProfile tp = muleContext.getDefaultThreadingProfile();
        final ThreadPoolFactory pf = tp.getPoolFactory();
        assertTrue(pf instanceof DefaultThreadPoolFactory);
    }
}
