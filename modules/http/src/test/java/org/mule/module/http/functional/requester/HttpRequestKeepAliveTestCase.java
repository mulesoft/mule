/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;
import static org.mule.module.http.api.HttpHeaders.Values.KEEP_ALIVE;
import org.mule.api.MuleEvent;
import org.mule.util.StringUtils;

import org.junit.Test;

public class HttpRequestKeepAliveTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-keep-alive-config.xml";
    }

    @Test
    public void persistentRequestSendsKeepAliveHeader() throws Exception
    {
        assertConnectionHeader("persistentRequestFlow", null, KEEP_ALIVE);
    }

    @Test
    public void nonPersistentRequestSendsCloseHeader() throws Exception
    {
        assertConnectionHeader("nonPersistentRequestFlow", null, CLOSE);
    }

    @Test
    public void persistentRequestWithKeepAlivePropertySendsKeepAliveHeader() throws Exception
    {
        assertConnectionHeader("persistentRequestFlow", KEEP_ALIVE, KEEP_ALIVE);
    }

    @Test
    public void nonPersistentRequestWithKeepAlivePropertySendsCloseHeader() throws Exception
    {
        assertConnectionHeader("nonPersistentRequestFlow", KEEP_ALIVE, CLOSE);
    }

    @Test
    public void nonPersistentRequestWithClosePropertySendsCloseHeader() throws Exception
    {
        assertConnectionHeader("nonPersistentRequestFlow", CLOSE, CLOSE);
    }


    private void assertConnectionHeader(String flow, String connectionOutboundProperty, String expectedConnectionHeader) throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        if (connectionOutboundProperty != null)
        {
            event.getMessage().setOutboundProperty(CONNECTION, connectionOutboundProperty);
        }
        runFlow(flow, event);
        String responseConnectionHeaderValue = StringUtils.join(headers.get(CONNECTION), " ");
        assertThat(responseConnectionHeaderValue, equalTo(expectedConnectionHeader));
    }

}
