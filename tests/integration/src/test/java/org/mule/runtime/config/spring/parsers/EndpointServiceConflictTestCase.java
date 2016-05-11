/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import org.junit.Test;

public class EndpointServiceConflictTestCase extends AbstractBadConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/endpoint-service-conflict-test-flow.xml";
    }

    @Test
    public void testBeanError() throws Exception
    {
        assertErrorContains("Two configuration elements have been defined with the same global name. Global name must be unique");
    }

}
