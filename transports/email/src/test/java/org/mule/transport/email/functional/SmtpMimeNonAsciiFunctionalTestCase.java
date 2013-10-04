/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
