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
import org.junit.Ignore;

public class SmtpNonAsciiFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpNonAsciiFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "smtp", configResources, Locale.JAPAN, "iso-2022-jp");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "smtp-functional-test-service.xml"},
            {ConfigVariant.FLOW, "smtp-functional-test-flow.xml"}
        });
    }      

    @Ignore("MULE-6926: Flaky test.")
    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

}
