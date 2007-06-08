/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.functional;

public class ImapStringFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapStringFunctionalTestCase()
    {
        super(65443, STRING_MESSAGE, "imap", "imap-string-functional-test.xml");
    }

    public void testReceive() throws Exception
    {
        doReceive();
    }

}