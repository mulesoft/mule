/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;

/**
 * This tests that we can have references to management context aware objects within a config
 */
public class MuleContextAwareTestCase extends AbstractServiceAndFlowTestCase
{

    public MuleContextAwareTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "management-context-aware-test-service.xml"},
            {ConfigVariant.FLOW, "management-context-aware-test-flow.xml"}
        });
    }

    @Test
    public void testStartup()
    {
        // only want startup to succeed
    }

}
