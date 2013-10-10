/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.issues;

import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import org.junit.Test;

public class Pop3WithConnectorMule2042TestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3WithConnectorMule2042TestCase()
    {
        super(STRING_MESSAGE, "pop3", "pop3-with-connector-mule-2042-test.xml");
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
