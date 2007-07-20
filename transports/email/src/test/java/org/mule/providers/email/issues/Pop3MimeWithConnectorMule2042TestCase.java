/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.issues;

import org.mule.providers.email.functional.AbstractEmailFunctionalTestCase;

public class Pop3MimeWithConnectorMule2042TestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3MimeWithConnectorMule2042TestCase()
    {
        super(65445, MIME_MESSAGE, "pop3", "pop3-mime-with-connector-mule-2042-test.xml");
    }

    public void testReceive() throws Exception
    {
        doReceive();
    }

}