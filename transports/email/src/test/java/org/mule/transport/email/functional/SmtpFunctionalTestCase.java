/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SmtpFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpFunctionalTestCase()
    {
        super(STRING_MESSAGE, "smtp");
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
