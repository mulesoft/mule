/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.interceptor;

import static org.apache.cxf.phase.Phase.PRE_LOGICAL;
import static org.mule.services.soap.client.SoapCxfClient.MULE_SOAP_ACTION;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * CXF interceptor that adds the SOAP action to the outgoing request message.
 *
 * @since 4.0
 */
public class SoapActionInterceptor extends AbstractPhaseInterceptor<Message> {

  public static final String SOAP_ACTION = "SOAPAction";

  public SoapActionInterceptor() {
    super(PRE_LOGICAL);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    String soapAction = (String) message.getExchange().get(MULE_SOAP_ACTION);
    message.put(SOAP_ACTION, soapAction);
  }
}
