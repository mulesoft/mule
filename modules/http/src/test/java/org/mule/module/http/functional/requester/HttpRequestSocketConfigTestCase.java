/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HttpRequestSocketConfigTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-socket-config.xml";
    }

    @Test
    public void globalClientSocketProperties() throws Exception
    {
        // For now, just test that the context is parsed correctly.
        runFlow("globalConfigFlow", TEST_MESSAGE);
        assertThat(body, equalTo(TEST_MESSAGE));
    }

    @Test
    public void nestedClientSocketProperties() throws Exception
    {
        // For now, just test that the context is parsed correctly.
        runFlow("nestedConfigFlow", TEST_MESSAGE);
        assertThat(body, equalTo(TEST_MESSAGE));
    }


}
