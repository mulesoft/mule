/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.endpoint;

import org.mule.config.spring.parsers.AbstractBadConfigTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class AttributeErrorTestCase extends AbstractBadConfigTestCase
{

    public AttributeErrorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);     
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/parsers/endpoint/attribute-error-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/config/spring/parsers/endpoint/attribute-error-test-flow.xml"}
        });
    }      
    
    @Test
    public void testError() throws Exception
    {
        assertErrorContains("do not match the exclusive groups [address] [ref]");
    }

}
