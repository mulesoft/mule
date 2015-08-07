/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

public class BasicJerseyWithExceptionMapperTestCase extends BasicJerseyTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "basic-exception-mapper-http-connector-conf-flow.xml";
    }

    @Override
    public void testThrowException() throws Exception
    {
        callThrowException(503, "This is an exception");
    }

}
