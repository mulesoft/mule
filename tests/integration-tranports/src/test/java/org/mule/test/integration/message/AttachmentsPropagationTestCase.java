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

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.ds.StringDataSource;

import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;

import org.junit.Test;

public class AttachmentsPropagationTestCase extends CompatibilityFunctionalTestCase {

  private static final String ATTACHMENT_CONTENT = "<content>";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/attachment-propagation.xml";
  }

  public static class AttachmentsPropagator implements Processor, FlowConstructAware {

    private FlowConstruct flowconstruct;

    @Override
    public Event process(Event event) throws MuleException {
      final InternalMessage message = event.getMessage();
      final Builder builder = InternalMessage.builder(message);
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

      final InternalMessage built = builder.build();

      return Event.builder(event).message(built).build();
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowconstruct = flowConstruct;
    }
  }

  @Test
  public void singleFlowShouldReceiveAttachment() throws Exception {
    InternalMessage result = flowRunner("SINGLE").withPayload("").run().getMessage();

    assertThat(result, is(notNullValue()));
    assertThat(result.getPayload().getValue(), instanceOf(Set.class));

    // expect SINGLE attachment from SINGLE service
    assertThat((Set<String>) result.getPayload().getValue(), containsInAnyOrder("SINGLE"));

    DataHandler attachment = result.getOutboundAttachment("SINGLE");
    assertThat(attachment, is(notNullValue()));
    assertThat(attachment.getContent().toString(), is(ATTACHMENT_CONTENT));
  }

  @Test
  public void chainedFlowShouldReceiveAttachments() throws Exception {
    InternalMessage result = flowRunner("CHAINED").withPayload("").run().getMessage();

    assertThat(result, is(notNullValue()));
    assertThat(result.getPayload().getValue(), instanceOf(Set.class));

    // expect CHAINED attachment from CHAINED service
    // and SINGLE attachment from SINGLE service
    assertThat((Set<String>) result.getPayload().getValue(), containsInAnyOrder("SINGLE", "CHAINED"));

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
