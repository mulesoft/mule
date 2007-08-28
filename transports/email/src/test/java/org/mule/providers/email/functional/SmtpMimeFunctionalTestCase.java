/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.functional;

public class SmtpMimeFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpMimeFunctionalTestCase()
    {
        super(65438, MIME_MESSAGE, "smtp", "smtp-mime-functional-test.xml");
    }

    public void testSend() throws Exception
    {
        doSend();
    }

}