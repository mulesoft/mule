/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class MultipleNamedTcpConnectorsTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public MultipleNamedTcpConnectorsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "multiple-named-tcp-connectors-test-service.xml"},
            {ConfigVariant.FLOW, "multiple-named-tcp-connectors-test-flow.xml"}});
    }

    @Test
    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.
    }

}
