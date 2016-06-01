/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestAttachmentsTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-request-attachment-config.xml";
    }

    /**
     * "Unsupported content type" means one that is not out of the box supported by javax.activation.
     */
    @Test
    public void inputStreamAttachmentWithUnsupportedContentType() throws Exception
    {
        final MuleEvent result = runFlow("attachmentFromBytes");
        assertThat(IOUtils.toString((InputStream) result.getMessage().getPayload()), is("OK"));
        FlowAssert.verify("reqWithAttachment");
    }

    /**
     * "Unsupported content type" means one that is not out of the box supported by javax.activation.
     */
    @Test
    public void byteArrayAttachmentWithUnsupportedContentType() throws Exception
    {
        final MuleEvent result = runFlow("attachmentFromStream");
        assertThat(IOUtils.toString((InputStream) result.getMessage().getPayload()), is("OK"));
        FlowAssert.verify("reqWithAttachment");
    }
}