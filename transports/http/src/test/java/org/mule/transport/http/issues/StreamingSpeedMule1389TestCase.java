/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.issues;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.util.SystemUtils;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;

public class StreamingSpeedMule1389TestCase extends AbstractStreamingCapacityTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public StreamingSpeedMule1389TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, 100 * ONE_MB);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "streaming-speed-mule-1389-service.xml"},
            {ConfigVariant.FLOW, "streaming-speed-mule-1389-flow.xml"}});
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        // MULE-4713
        return (SystemUtils.isIbmJDK() && SystemUtils.isJavaVersionAtLeast(160));
    }
}
