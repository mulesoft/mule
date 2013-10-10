/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import org.junit.Test;

public class SmtpMimeFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpMimeFunctionalTestCase()
    {
        super(MIME_MESSAGE, "smtp", "smtp-mime-functional-test.xml");
    }

    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

}
