/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.functional;

import java.util.Locale;

public class SmtpNonAsciiFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpNonAsciiFunctionalTestCase()
    {
        super(65437, STRING_MESSAGE, "smtp", Locale.JAPAN, "iso-2022-jp");
    }

    public void testSend() throws Exception
    {
        doSend();
    }

}