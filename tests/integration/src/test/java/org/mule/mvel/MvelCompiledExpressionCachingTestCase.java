/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mvel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class MvelCompiledExpressionCachingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "mvel-compiled-expression-caching-config.xml";
    }

    @Test
    public void testIssue() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://fooInput", TEST_MESSAGE, null);

        assertThat(response.getPayloadAsString(), equalTo("SUCCESS"));
    }
}
