/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that adds the SOAP action to the message.
 */
public class SoapActionInterceptor extends AbstractPhaseInterceptor<Message> {

  private final String soapAction;

  public SoapActionInterceptor(String soapAction) {
    super(Phase.PRE_LOGICAL);
    this.soapAction = soapAction;
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    message.put("SOAPAction", soapAction);
  }
}
