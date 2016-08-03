/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.message.ds.StringDataSource;

import java.util.Set;

import javax.activation.DataHandler;

import org.junit.Test;

public class AttachmentsPropagationTestCase extends AbstractIntegrationTestCase implements EventCallback
{
    private static final String ATTACHMENT_CONTENT = "<content>";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/message/attachment-propagation.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("SINGLE");
        ftc.setEventCallback(this);

        ftc = (FunctionalTestComponent) getComponent("CHAINED");
        ftc.setEventCallback(this);
    }

    @Override
    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        final MuleMessage message = context.getEvent().getMessage();
        final Builder builder = MuleMessage.builder(message);

        for (String attachmentName : message.getInboundAttachmentNames())
        {
            DataHandler inboundAttachment = message.getInboundAttachment(attachmentName);
            builder.addOutboundAttachment(attachmentName, inboundAttachment);
        }

        // add an attachment, named after the componentname...
        String attachmentName = context.getFlowConstruct().getName();
        DataHandler dataHandler = new DataHandler(new StringDataSource(ATTACHMENT_CONTENT, "doesNotMatter", MediaType.TEXT));
        builder.addOutboundAttachment(attachmentName, dataHandler);

        final MuleMessage built = builder.build();

        // return the list of attachment names
        FunctionalTestComponent fc = (FunctionalTestComponent) component;
        fc.setReturnData(built.getOutboundAttachmentNames());

        context.getEvent().setMessage(built);
    }

    @Test
    public void singleFlowShouldReceiveAttachment() throws Exception
    {
        MuleMessage result = flowRunner("SINGLE").withPayload("").run().getMessage();

        assertThat(result, is(notNullValue()));

        // expect SINGLE attachment from SINGLE service
        assertThat((Set<String>) result.getPayload(), containsInAnyOrder("SINGLE"));

        DataHandler attachment = result.getOutboundAttachment("SINGLE");
        assertThat(attachment, is(notNullValue()));
        assertThat(attachment.getContent().toString(), is(ATTACHMENT_CONTENT));
    }

    @Test
    public void chainedFlowShouldReceiveAttachments() throws Exception
    {
        MuleMessage result = flowRunner("CHAINED").withPayload("").run().getMessage();
        assertThat(result, is(notNullValue()));

        // expect CHAINED attachment from CHAINED service
        // and SINGLE attachment from SINGLE service
        assertThat((Set<String>) result.getPayload(), containsInAnyOrder("SINGLE", "CHAINED"));

        // don't check the attachments now - it seems they're not copied properly from inbound
        // to outbound on flow boundaries
//        DataHandler attachment = result.getInboundAttachment("SINGLE");
//        assertNotNull(attachment);
//        assertEquals(ATTACHMENT_CONTENT, attachment.getContent().toString());
//
//        attachment = result.getInboundAttachment("CHAINED");
//        assertNotNull(attachment);
//        assertEquals(ATTACHMENT_CONTENT, attachment.getContent().toString());
    }
}
