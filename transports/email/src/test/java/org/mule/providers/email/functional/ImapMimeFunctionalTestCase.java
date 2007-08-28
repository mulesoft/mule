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

public class ImapMimeFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapMimeFunctionalTestCase()
    {
        super(65443, MIME_MESSAGE, "imap", "imap-mime-functional-test.xml");
    }

    public void testReceive() throws Exception
    {
        doReceive();
    }

}