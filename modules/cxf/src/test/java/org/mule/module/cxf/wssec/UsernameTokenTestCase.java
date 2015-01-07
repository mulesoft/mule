/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UsernameTokenTestCase extends FunctionalTestCase
{
    private Latch greetLatch;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameter
    public String[] configFiles;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]
        {
                {new String[] {"org/mule/module/cxf/wssec/cxf-secure-service-flow.xml", "org/mule/module/cxf/wssec/username-token-conf.xml"}},
                {new String[] {"org/mule/module/cxf/wssec/cxf-secure-service-flow-httpn.xml", "org/mule/module/cxf/wssec/username-token-conf.xml"}}
        });
    }
    
    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
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


