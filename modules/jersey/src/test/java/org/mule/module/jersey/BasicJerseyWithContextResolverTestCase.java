/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class BasicJerseyWithContextResolverTestCase extends BasicJerseyTestCase
{
    public BasicJerseyWithContextResolverTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "basic-context-resolver-conf-service.xml"},
                {ConfigVariant.FLOW, "basic-context-resolver-conf-flow.xml"},
                {ConfigVariant.FLOW, "basic-context-resolver-http-connector-conf-flow.xml"}
        });
    }

    @Override
    public void testThrowException() throws Exception
    {
        callThrowException(INTERNAL_SERVER_ERROR.getStatusCode(), "This is an exception");
    }

    @Override
    protected String getJsonHelloBean()
    {
        //note the number 0 is not enclosed in quotes
        return "{\"message\":\"Hello Dan\",\"number\":0}";
    }
}
