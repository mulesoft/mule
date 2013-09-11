/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
