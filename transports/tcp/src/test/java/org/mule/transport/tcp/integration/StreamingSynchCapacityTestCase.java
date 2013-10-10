/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.SystemUtils;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests a model for which synchonous=true for environment (was "and connector", but
 * that is no longer possible). Not the same as issue MULE-1687. This will happily
 * send 1GB while running in significantly less memory, but it takes some time. Since
 * I'd like this to run in CI I will set at 100MB and test memory delta. But since
 * memory usage could be around that anyway, this is may be a little unreliable. And
 * there's no way to measure memory use directly in 1.4. We'll see... IMPORTANT - DO
 * NOT RUN THIS TEST IN AN IDE WITH LOG LEVEL OF DEBUG. USE INFO TO SEE DIAGNOSTICS.
 * OTHERWISE THE CONSOLE OUTPUT WILL BE SIMILAR SIZE TO DATA TRANSFERRED, CAUSING
 * CONFUSNG AND PROBABLY FATAL MEMORY USE.
 */
public class StreamingSynchCapacityTestCase extends AbstractStreamingCapacityTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-streaming2-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-streaming2-test-flow.xml"}
        });
    }

    public StreamingSynchCapacityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, 100 * ONE_MB);
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        // MULE-4713
        return (SystemUtils.isIbmJDK() && SystemUtils.isJavaVersionAtLeast(160));
    }
}
