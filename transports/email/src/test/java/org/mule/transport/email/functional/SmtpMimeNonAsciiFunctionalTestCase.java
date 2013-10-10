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

public class SmtpMimeNonAsciiFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpMimeNonAsciiFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, MIME_MESSAGE, "smtp", configResources, Locale.JAPAN, "iso-2022-jp");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "smtp-mime-functional-test-service.xml"},
            {ConfigVariant.FLOW, "smtp-mime-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

}
