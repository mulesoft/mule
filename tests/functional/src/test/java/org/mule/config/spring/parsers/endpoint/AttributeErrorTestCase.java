/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
