/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.interceptor;

import static org.apache.cxf.interceptor.Fault.FAULT_CODE_SERVER;
import static org.apache.cxf.phase.Phase.PRE_PROTOCOL;
import static org.mule.services.soap.client.SoapCxfClient.MULE_ATTACHMENTS_KEY;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.services.soap.api.message.SoapRequest;
import org.mule.services.soap.client.SoapCxfClient;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Collection;

import javax.activation.DataHandler;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * CXF out interceptor that collects the received Mtom SOAP attachments in the response, transforms it to message parts and stores
 * them in the response message {@link Exchange} so then can be returned by the {@link SoapCxfClient#consume(SoapRequest)} as a
 * {@link MultiPartPayload}, if no attachments are returned an empty map is set.
 *
 * @since 4.0
 */
public class OutputMtomSoapAttachmentsInterceptor extends AbstractPhaseInterceptor<Message> {

  public OutputMtomSoapAttachmentsInterceptor() {
    super(PRE_PROTOCOL);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    final ImmutableMap.Builder<String, SoapAttachment> result = ImmutableMap.builder();
    Collection<Attachment> attachments = message.getAttachments();
    if (attachments != null && !attachments.isEmpty()) {
      attachments.forEach(a -> result.put(getName(a), getSoapAttachment(a)));
    }
    message.getExchange().put(MULE_ATTACHMENTS_KEY, result.build());
  }

  private String getName(Attachment attachment) {
    DataHandler dataHandler = attachment.getDataHandler();
    return dataHandler.getName() != null ? dataHandler.getName() : attachment.getId();
  }

  private SoapAttachment getSoapAttachment(Attachment attachment) {
    DataHandler dataHandler = attachment.getDataHandler();
    try {
      MediaType contentType = MediaType.parse(dataHandler.getContentType());
      return new SoapAttachment(dataHandler.getInputStream(), contentType);
    } catch (IOException e) {
      throw new SoapFault("Error copying received attachment [" + getName(attachment) + "]", e, FAULT_CODE_SERVER);
    }
  }
}
