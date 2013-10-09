/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
