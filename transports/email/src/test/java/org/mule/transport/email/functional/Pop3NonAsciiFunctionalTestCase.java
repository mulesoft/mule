/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.functional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class Pop3NonAsciiFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3NonAsciiFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "pop3", configResources, Locale.JAPAN, "iso-2022-jp");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "pop3-functional-test-service.xml"},
            {ConfigVariant.FLOW, "pop3-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
