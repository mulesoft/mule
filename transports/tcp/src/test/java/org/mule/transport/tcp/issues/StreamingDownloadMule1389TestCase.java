/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.issues;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.junit4.rule.DynamicPort;

/**
 * This fails to work as described in the issue, but isn't HTTP...
 */
public class StreamingDownloadMule1389TestCase extends AbstractStreamingDownloadMule1389TestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public StreamingDownloadMule1389TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "streaming-download-mule-1389-service.xml"},
            {ConfigVariant.FLOW, "streaming-download-mule-1389-flow.xml"}});
    }
}
