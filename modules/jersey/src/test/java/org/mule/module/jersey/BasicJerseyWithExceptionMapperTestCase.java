/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class BasicJerseyWithExceptionMapperTestCase extends BasicJerseyTestCase
{

    public BasicJerseyWithExceptionMapperTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "basic-exception-mapper-conf-service.xml"},
                {ConfigVariant.FLOW, "basic-exception-mapper-conf-flow.xml"},
                {ConfigVariant.FLOW, "basic-exception-mapper-http-connector-conf-flow.xml"}
        });
    }      
    
    @Override
    public void testThrowException() throws Exception
    {
        callThrowException(503, "This is an exception");
    }

}
