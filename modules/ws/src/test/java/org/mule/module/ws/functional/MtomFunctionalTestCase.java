/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

import org.apache.cxf.attachment.AttachmentImpl;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Test;


public class MtomFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    private static final String TEST_FILE_ATTACHMENT = "TestAttachments.wsdl";

    @Override
    protected String getConfigFile()
    {
        return "mtom-config.xml";
    }

    @Test
    public void uploadAttachmentTest() throws Exception
    {
        String request = String.format("<ns:uploadAttachment xmlns:ns=\"http://consumer.ws.module.mule.org/\">" +
                                       "<fileName>%s</fileName><attachment>" +
                                       "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:testAttachmentId\"/>" +
                                       "</attachment></ns:uploadAttachment>", TEST_FILE_ATTACHMENT);

        Flow flow = (Flow) getFlowConstruct("clientUploadAttachment");
        MuleEvent event = getTestEvent(request);

        addAttachment(event.getMessage(), TEST_FILE_ATTACHMENT, "testAttachmentId");

        event = flow.process(event);

        String expected = "<ns2:uploadAttachmentResponse xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                          "<result>OK</result></ns2:uploadAttachmentResponse>";

        assertXMLEqual(expected, event.getMessage().getPayloadAsString());
        assertThat(event.getMessage().getInvocationProperty("cxf_attachments"), notNullValue());
        AttachmentImpl collection[] = ((Collection<AttachmentImpl>) event.getMessage().getInvocationProperty("cxf_attachments"))
                .toArray(new AttachmentImpl[] {});
        assertThat(collection.length, is(1));
        assertThat(collection[0].getHeader("Content-Disposition"), containsString("attachment"));
    }

    @Test
    public void downloadAttachmentTest() throws Exception
    {
        String request = String.format("<ns:downloadAttachment xmlns:ns=\"http://consumer.ws.module.mule.org/\">" +
                                       "<fileName>%s</fileName></ns:downloadAttachment>", TEST_FILE_ATTACHMENT);

        Flow flow = (Flow) getFlowConstruct("clientDownloadAttachment");
        MuleEvent event = getTestEvent(request);

        event = flow.process(event);

        assertAttachmentInResponse(event.getMessage(), TEST_FILE_ATTACHMENT);
    }

    @Test
    public void echoAttachment() throws Exception
    {
        String request = "<ns:echoAttachment xmlns:ns=\"http://consumer.ws.module.mule.org/\"><attachment>" +
                         "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:testAttachmentId\"/>" +
                         "</attachment></ns:echoAttachment>";

        Flow flow = (Flow) getFlowConstruct("clientEchoAttachment");
        MuleEvent event = getTestEvent(request);

        addAttachment(event.getMessage(), TEST_FILE_ATTACHMENT, "testAttachmentId");
        event = flow.process(event);

        assertAttachmentInResponse(event.getMessage(), TEST_FILE_ATTACHMENT);
        assertThat(event.getMessage().getInvocationProperty("cxf_attachments"), notNullValue());
        AttachmentImpl collection[] = ((Collection<AttachmentImpl>) event.getMessage().getInvocationProperty("cxf_attachments"))
                .toArray(new AttachmentImpl[] {});
        assertThat(collection.length, is(1));
        assertThat(collection[0].getHeader("Content-Disposition"), containsString("attachment"));
    }

    private void addAttachment(MuleMessage message, String fileName, String attachmentId) throws Exception
    {
        File file = new File(IOUtils.getResourceAsUrl(fileName, getClass()).getPath());
        DataHandler dh = new DataHandler(new FileDataSource(file));
        message.addOutboundAttachment(attachmentId, dh);
    }

    private void assertAttachmentInResponse(MuleMessage message, String fileName) throws Exception
    {
        assertThat(message.getInboundAttachmentNames(), hasSize(1));

        String attachmentId = extractAttachmentId(message.getPayloadAsString());
        DataHandler attachment = message.getInboundAttachment(attachmentId);

        assertThat(attachment, notNullValue());

        InputStream expected = IOUtils.getResourceAsStream(fileName, getClass());

        assertThat(IOUtils.contentEquals(expected, attachment.getInputStream()), is(true));
    }


    private String extractAttachmentId(String payload)
    {
        Pattern pattern = Pattern.compile("href=\"cid:(.*?)\"");
        Matcher matcher = pattern.matcher(payload);

        if (matcher.find())
        {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Payload does not contain an attachment id");
    }


}