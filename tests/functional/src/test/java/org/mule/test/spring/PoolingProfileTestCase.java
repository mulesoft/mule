/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.config.PoolingProfile;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMODescriptor;

// TODO Get the PoolingProfile from the PoolingObjectFactory now.  Update the config to use scope="pooled"
public class PoolingProfileTestCase  extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "pooling-profile-test.xml";
    }

    public void testDefault()
    {
//        doTest("default", PoolingProfile.DEFAULT_POOL_EXHAUSTED_ACTION,
//                PoolingProfile.DEFAULT_POOL_INITIALISATION_POLICY,
//                PoolingProfile.DEFAULT_MAX_POOL_ACTIVE,
//                PoolingProfile.DEFAULT_MAX_POOL_IDLE,
//                PoolingProfile.DEFAULT_MAX_POOL_WAIT);
    }
//
//    public void testFailAll()
//    {
//        doTest("fail_all", PoolingProfile.WHEN_EXHAUSTED_FAIL,
//                PoolingProfile.INITIALISE_ALL, 1, 2, 3);
//    }
//
//    public void testGrowOne()
//    {
//        doTest("grow_one", PoolingProfile.WHEN_EXHAUSTED_GROW,
//                PoolingProfile.INITIALISE_ONE, 2, 3, 4);
//    }
//
//    public void testWaitNone()
//    {
//        doTest("wait_none", PoolingProfile.WHEN_EXHAUSTED_WAIT,
//                PoolingProfile.INITIALISE_NONE, 3, 4, 5);
//    }
//
//    protected void doTest(String service, int exhausted, int initialisation,
//                          int active, int idle, long wait)
//    {
//        UMODescriptor descriptor = managementContext.getRegistry().lookupService(service);
//        assertNotNull(service, descriptor);
//        PoolingProfile profile = descriptor.getPoolingProfile();
//        assertNotNull(profile);
//        assertEquals("exhausted:", exhausted, profile.getExhaustedAction());
//        assertEquals("initialisation:", initialisation, profile.getInitialisationPolicy());
//        assertEquals("active:", active, profile.getMaxActive());
//        assertEquals("idle:", idle, profile.getMaxIdle());
//        assertEquals("wait:", wait, profile.getMaxWait());
//    }
}
