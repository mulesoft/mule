/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.issues;

import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import org.junit.Test;

public class Pop3MimeWithConnectorMule2042TestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3MimeWithConnectorMule2042TestCase()
    {
        super(MIME_MESSAGE, "pop3");
    }

    @Override
    protected String getConfigFile()
    {
        return "pop3-mime-with-connector-mule-2042-test-flow.xml";
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
