/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.runtime.core.config.PoolingProfile;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class PoolingProfileTestCase extends AbstractIntegrationTestCase
{

    private static boolean evicted;

    @Override
    protected String getConfigFile()
    {
        return "pooling-profile-test-flow.xml";
    }

    @Test
    public void testDefault()
    {
        doTest("default", PoolingProfile.DEFAULT_POOL_EXHAUSTED_ACTION,
                PoolingProfile.DEFAULT_POOL_INITIALISATION_POLICY,
                PoolingProfile.DEFAULT_MAX_POOL_ACTIVE,
                PoolingProfile.DEFAULT_MAX_POOL_IDLE,
                PoolingProfile.DEFAULT_MAX_POOL_WAIT);
    }

    @Test
    public void testFailAll()
    {
        doTest("fail_all", PoolingProfile.WHEN_EXHAUSTED_FAIL,
                PoolingProfile.INITIALISE_ALL, 1, 2, 3);
    }

    @Test
    public void testGrowOne()
    {
        doTest("grow_one", PoolingProfile.WHEN_EXHAUSTED_GROW,
                PoolingProfile.INITIALISE_ONE, 2, 3, 4);
    }

    @Test
    public void testWaitNone()
    {
        doTest("wait_none", PoolingProfile.WHEN_EXHAUSTED_WAIT,
                PoolingProfile.INITIALISE_NONE, 3, 4, 5);
    }

    @Test
    public void testEvictOne()
    {
        doTest("evict_one", PoolingProfile.WHEN_EXHAUSTED_WAIT,
               PoolingProfile.INITIALISE_ALL, 1, 1, 0);


        Prober prober = new PollingProber(5000, 50);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return evicted;
            }

            public String describeFailure()
            {
                return "Pooled component was not evicted";
            }
        });
    }

    protected void doTest(String serviceFlow, int exhausted, int initialisation,
                          int active, int idle, long wait)
    {
        Object o = muleContext.getRegistry().lookupObject(serviceFlow);
        assertNotNull(serviceFlow, o);


        assertTrue(((Flow) o).getMessageProcessors().get(0) instanceof PooledJavaComponent);
        PooledJavaComponent pjc = (PooledJavaComponent) ((Flow) o).getMessageProcessors().get(0);

        PoolingProfile profile = pjc.getPoolingProfile();
        assertNotNull(profile);
        assertEquals("exhausted:", exhausted, profile.getExhaustedAction());
        assertEquals("initialisation:", initialisation, profile.getInitialisationPolicy());
        assertEquals("active:", active, profile.getMaxActive());
        assertEquals("idle:", idle, profile.getMaxIdle());
        assertEquals("wait:", wait, profile.getMaxWait());
    }

    public static class EvictablePooledComponent extends FunctionalTestComponent
    {

        @Override
        public void dispose()
        {
            super.dispose();
            evicted = true;
        }
    }
}
