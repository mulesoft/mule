/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.interceptor;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.cxf.phase.Phase.PRE_PROTOCOL;
import static org.mule.services.soap.impl.client.SoapCxfClient.MULE_HEADERS_KEY;
import org.mule.services.soap.api.message.SoapResponse;
import org.mule.services.soap.impl.exception.BadResponseException;
import org.mule.services.soap.impl.util.XmlTransformationException;
import org.mule.services.soap.impl.util.XmlTransformationUtils;

import java.util.Map;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.w3c.dom.Node;

/**
 * CXF out interceptor that collects the received SOAP headers in the response, transforms it and stores them in the response
 * message {@link Exchange} so then can be returned in a {@link SoapResponse}.
 *
 * @since 4.0
 */
public class OutputSoapHeadersInterceptor extends AbstractSoapInterceptor {

  public OutputSoapHeadersInterceptor() {
    super(PRE_PROTOCOL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleMessage(SoapMessage message) throws Fault {
    Map<String, String> result = message.getHeaders().stream()
        .filter(header -> header instanceof SoapHeader)
        .collect(toMap(h -> h.getName().getLocalPart(), h -> {
          try {
            return XmlTransformationUtils.nodeToString((Node) h.getObject());
          } catch (XmlTransformationException e) {
            throw new BadResponseException(format("Error while processing response header [%s]", h.getName()), e);
          }
        }));
    message.getExchange().put(MULE_HEADERS_KEY, result);
  }
}
