/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import org.junit.Test;

public class ConstrainedConnectorExceptionStrategyMule2126TestCase extends AbstractBadConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/constrained-connector-exception-strategy-mule-2126-test.xml";
    }

    @Test
    public void testError() throws Exception
    {
        assertErrorContains("Invalid content was found starting with element 'default-connector-exception-strategy'");
    }

}
