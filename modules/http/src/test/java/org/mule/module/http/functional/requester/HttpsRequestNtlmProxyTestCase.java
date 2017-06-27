/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.api.MuleEvent;

import org.junit.Test;

public class HttpsRequestNtlmProxyTestCase extends AbstractNtlmHttpToHttpsTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "https-request-ntlm-proxy-config.xml";
    }

    @Test
    public void validNtlmAuth() throws Exception
    {
        MuleEvent event = runFlow(getFlowName());
        assertThat((int) event.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
        assertThat(event.getMessage().getPayloadAsString(), equalTo(TARGET_SERVER_RESPONSE));
    }
    
    private String getFlowName()
    {
        return "ntlmFlow";
    }
}
