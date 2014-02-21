/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import java.util.Locale;

import org.junit.Test;

public class SmtpMimeNonAsciiFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpMimeNonAsciiFunctionalTestCase()
    {
        super(MIME_MESSAGE, "smtp", Locale.JAPAN, "iso-2022-jp");
    }

    @Override
    protected String getConfigFile()
    {
        return "smtp-mime-functional-test-flow.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

}
