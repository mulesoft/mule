/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import org.junit.Test;

public class Pop3FunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3FunctionalTestCase()
    {
        super(STRING_MESSAGE, "pop3");
    }

    @Override
    protected String getConfigFile()
    {
        return "pop3-functional-test-flow.xml";
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
