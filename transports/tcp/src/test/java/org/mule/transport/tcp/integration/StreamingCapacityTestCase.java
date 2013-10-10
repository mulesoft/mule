/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;

/**
 * This will happily send 1GB while running in significantly less memory, but it takes some time.
 * Since I'd like this to run in CI I will set at 100MB and test memory delta.  But since memory usage
 * could be around that anyway, this is may be a little unreliable.  And there's no way to
 * measure memory use directly in 1.4.  We'll see...
 *
 * IMPORTANT - DO NOT RUN THIS TEST IN AN IDE WITH LOG LEVEL OF DEBUG.  USE INFO TO SEE
 * DIAGNOSTICS.  OTHERWISE THE CONSOLE OUTPUT WILL BE SIMILAR SIZE TO DATA TRANSFERRED,
 * CAUSING CONFUSNG AND PROBABLY FATAL MEMORY USE.
 */
public class StreamingCapacityTestCase extends AbstractStreamingCapacityTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");    

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-streaming-test-service.xml", 10 * ONE_GB},
            {ConfigVariant.FLOW, "tcp-streaming-test-flow.xml", 10 * ONE_GB}});
    }

    public StreamingCapacityTestCase(ConfigVariant variant, String configResources, long size)
    {
        super(variant, configResources, size);
    }
}

