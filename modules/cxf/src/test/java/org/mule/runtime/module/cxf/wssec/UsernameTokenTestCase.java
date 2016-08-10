/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

public class UsernameTokenTestCase extends FunctionalTestCase
{
    private Latch greetLatch;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                             "org/mule/runtime/module/cxf/wssec/cxf-secure-service-flow-httpn.xml",
                             "org/mule/runtime/module/cxf/wssec/username-token-conf.xml"
        };
    }

    @Override
    protected void doSetUp() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
        super.doSetUp();

        greetLatch = getGreeter().getLatch();
    }

    @Test
    public void testUsernameToken() throws Exception
    {
        assertTrue(greetLatch.await(60, TimeUnit.SECONDS));
    }

    protected GreeterWithLatch getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");
        return (GreeterWithLatch) instance;
    }
}


