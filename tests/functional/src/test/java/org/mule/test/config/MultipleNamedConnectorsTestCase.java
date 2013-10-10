/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class MultipleNamedConnectorsTestCase extends AbstractServiceAndFlowTestCase
{

    public MultipleNamedConnectorsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "multiple-named-connectors-test-service.xml"},
            {ConfigVariant.FLOW, "multiple-named-connectors-test-flow.xml"}
        });
    }

    @Test
    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.   
    }
}
