/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import java.util.Arrays;
import java.util.Collection;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertEquals;

public class SmtpFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "smtp", configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "smtp-functional-test-service.xml"},
            {ConfigVariant.FLOW, "smtp-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

    @Override
    public void verifyMessage(MimeMessage message) throws Exception
    {
        super.verifyMessage(message);
        assertEquals(DEFAULT_MESSAGE, message.getSubject());
    }

}
