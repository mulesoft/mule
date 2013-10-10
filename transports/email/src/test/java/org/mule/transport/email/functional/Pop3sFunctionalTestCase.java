/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class Pop3sFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3sFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "pop3s", configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "pop3s-functional-test-service.xml"},
            {ConfigVariant.FLOW, "pop3s-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
