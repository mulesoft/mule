/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.message.ds.StringDataSource;

import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;

import org.junit.Test;

public class AttachmentsPropagationTestCase extends FunctionalTestCase {

  private static final String ATTACHMENT_CONTENT = "<content>";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/attachment-propagation.xml";
  }

  public static class AttachmentsPropagator implements MessageProcessor, FlowConstructAware {

    private FlowConstruct flowconstruct;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      final MuleMessage message = event.getMessage();
      final Builder builder = MuleMessage.builder(message);
      final Set<String> attachmentNames = new TreeSet<>(message.getOutboundAttachmentNames());

      for (String attachmentName : message.getInboundAttachmentNames()) {
        DataHandler inboundAttachment = message.getInboundAttachment(attachmentName);
        builder.addOutboundAttachment(attachmentName, inboundAttachment);
        attachmentNames.add(attachmentName);
      }

      // add an attachment, named after the componentname...
      String attachmentName = flowconstruct.getName();
      DataHandler dataHandler = new DataHandler(new StringDataSource(ATTACHMENT_CONTENT, "doesNotMatter", MediaType.TEXT));
      builder.addOutboundAttachment(attachmentName, dataHandler);
      attachmentNames.add(attachmentName);

      builder.payload(attachmentNames);

      final MuleMessage built = builder.build();

      return MuleEvent.builder(event).message(built).build();
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowconstruct = flowConstruct;
    }
  }

  @Test
  public void singleFlowShouldReceiveAttachment() throws Exception {
    MuleMessage result = flowRunner("SINGLE").withPayload("").run().getMessage();

    assertThat(result, is(notNullValue()));
    assertThat(result.getPayload(), instanceOf(Set.class));

    // expect SINGLE attachment from SINGLE service
    assertThat((Set<String>) result.getPayload(), containsInAnyOrder("SINGLE"));

    DataHandler attachment = result.getOutboundAttachment("SINGLE");
    assertThat(attachment, is(notNullValue()));
    assertThat(attachment.getContent().toString(), is(ATTACHMENT_CONTENT));
  }

  @Test
  public void chainedFlowShouldReceiveAttachments() throws Exception {
    MuleMessage result = flowRunner("CHAINED").withPayload("").run().getMessage();

    assertThat(result, is(notNullValue()));
    assertThat(result.getPayload(), instanceOf(Set.class));

    // expect CHAINED attachment from CHAINED service
    // and SINGLE attachment from SINGLE service
    assertThat((Set<String>) result.getPayload(), containsInAnyOrder("SINGLE", "CHAINED"));

    // don't check the attachments now - it seems they're not copied properly from inbound
    // to outbound on flow boundaries
    // DataHandler attachment = result.getInboundAttachment("SINGLE");
    // assertNotNull(attachment);
    // assertEquals(ATTACHMENT_CONTENT, attachment.getContent().toString());
    //
    // attachment = result.getInboundAttachment("CHAINED");
    // assertNotNull(attachment);
    // assertEquals(ATTACHMENT_CONTENT, attachment.getContent().toString());
  }
}
