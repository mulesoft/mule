/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "basic-exception-mapper-conf-service.xml"},
            {ConfigVariant.FLOW, "basic-exception-mapper-conf-flow.xml"}
        });
    }      
    
    @Override
    public void testThrowException() throws Exception
    {
        callThrowException(503, "This is an exception");
    }

}
