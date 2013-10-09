/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
