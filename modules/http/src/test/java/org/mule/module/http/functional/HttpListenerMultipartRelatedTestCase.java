/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional;

import static java.util.Collections.singleton;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_ID;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.http.internal.multipart.HttpPart;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerMultipartRelatedTestCase extends FunctionalTestCase
{

    private static final String CONTENT = "test";

    private static final String CONTENT_ID_VALUE = "someContentId";
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-multipart-related-config.xml";
    }

    @Test
    public void testMultipartRelatedWithContentID() throws Exception
    {
        MuleEvent request = getTestEvent(null);
        request.getMessage().setOutboundProperty("Content-Type", "multipart/related");
        setAttachments(request.getMessage());
        MuleEvent event = runFlow("testMultipartRelated", request);
        assertThat(event.getMessage().getPayloadAsString(), is(CONTENT));
    }


    private void setAttachments(MuleMessage message) throws Exception
    {
        HttpPart part = new HttpPart(null, CONTENT.getBytes(), "text/plain", CONTENT.length());
        part.addHeader(CONTENT_ID, CONTENT_ID_VALUE);
        HttpPartDataSource httpPartDataSource = new ArrayList<>(HttpPartDataSource.createFrom(singleton(part))).get(0);
        DataHandler dh = new DataHandler(httpPartDataSource);
        message.addOutboundAttachment("attachment", dh);
    }

}
