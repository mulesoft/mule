/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileAttachmentNameTestCase extends FunctionalTestCase
{

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "file-attachment-name-config.xml";
    }

    @Test
    public void keepsAttachmentAndFileNames() throws Exception
    {
        MuleClient client = muleContext.getClient();
        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(TEST_MESSAGE.getBytes(), "text/xml", "testAttachment.txt"));
        MuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.addOutboundAttachment("testAttachment", dataHandler);

        MuleMessage response = client.send("http://localhost:" + httpPort.getValue() + "/testInput", msg);

        assertThat(getPayloadAsString(response), equalTo("testAttachment:testAttachment.txt"));
    }

    @Override
    public int getTestTimeoutSecs()
    {
        // TODO Auto-generated method stub
        return 100 * super.getTestTimeoutSecs();
    }
}
