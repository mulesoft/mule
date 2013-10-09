/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.functional;


import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ImapsFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapsFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "imaps", configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "imaps-functional-test-service.xml"},
            {ConfigVariant.FLOW, "imaps-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }
}
