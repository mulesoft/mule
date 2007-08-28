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

public class Pop3sFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3sFunctionalTestCase()
    {
        super(65436, STRING_MESSAGE, "pop3s");
    }

    public void testReceive() throws Exception
    {
        doReceive();
    }

}