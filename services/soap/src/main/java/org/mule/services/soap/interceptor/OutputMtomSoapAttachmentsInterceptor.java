/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.interceptor;

import static java.util.Collections.emptyList;
import static org.apache.cxf.interceptor.Fault.FAULT_CODE_SERVER;
import static org.apache.cxf.phase.Phase.PRE_PROTOCOL;
import static org.mule.services.soap.client.SoapCxfClient.MULE_ATTACHMENTS_KEY;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.services.soap.api.message.ImmutableSoapAttachment;
import org.mule.services.soap.api.message.SoapAttachment;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.activation.DataHandler;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * CXF out interceptor that collects the received Mtom SOAP attachments in the response, transforms it to message parts and stores
 * them in the response message {@link Exchange} so then can be returned by the {@link ConsumeOperation} as a
 * {@link MultiPartPayload}, if no attachments are returned an empty list is set.
 *
 * @since 4.0
 */
public class OutputMtomSoapAttachmentsInterceptor extends AbstractPhaseInterceptor<Message> {

  public OutputMtomSoapAttachmentsInterceptor() {
    super(PRE_PROTOCOL);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    Collection<Attachment> attachments = message.getAttachments();
    List attachmentParts = emptyList();
    if (attachments != null && !attachments.isEmpty()) {
      attachmentParts = attachments.stream().map(this::createSoapAttachment).collect(new ImmutableListCollector<>());
    }
    message.getExchange().put(MULE_ATTACHMENTS_KEY, attachmentParts);
  }

  private SoapAttachment createSoapAttachment(Attachment attachment) {
    DataHandler dataHandler = attachment.getDataHandler();
    String name = dataHandler.getName() != null ? dataHandler.getName() : attachment.getId();
    try {
      return new ImmutableSoapAttachment(name, dataHandler.getContentType(), dataHandler.getInputStream());
    } catch (IOException e) {
      throw new SoapFault("Error copying received attachment [" + name + "]", e, FAULT_CODE_SERVER);
    }
  }
}
