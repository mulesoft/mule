/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.issues;

import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SmtpEncodingMule7445TestCase extends AbstractEmailFunctionalTestCase
{

    private static final String MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

    public SmtpEncodingMule7445TestCase()
    {
        super(STRING_MESSAGE, "smtp", DEFAULT_EMAIL, DEFAULT_USER, MESSAGE, DEFAULT_PASSWORD, null);
    }

    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

}
