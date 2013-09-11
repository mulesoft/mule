/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ConstrainedConnectorExceptionStrategyMule2126TestCase extends AbstractBadConfigTestCase
{

    public ConstrainedConnectorExceptionStrategyMule2126TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/parsers/constrained-connector-exception-strategy-mule-2126-test.xml"}           
        });
    }      
    
    @Test
    public void testError() throws Exception
    {
        assertErrorContains("Invalid content was found starting with element 'default-connector-exception-strategy'");
    }

}
