/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.interceptor;

import static org.mule.extension.ws.internal.ConsumeOperation.MULE_SOAP_ACTION;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that adds the SOAP action to the outgoing request message.
 *
 * @since 4.0
 */
public class SoapActionInterceptor extends AbstractPhaseInterceptor<Message> {


  public SoapActionInterceptor() {
    super(Phase.PRE_LOGICAL);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    String soapAction = (String) message.getExchange().get(MULE_SOAP_ACTION);
    message.put("SOAPAction", soapAction);
  }
}
