/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.connector.EndpointConnectException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.util.StringUtils;

public class HttpsMessageReceiver extends HttpMessageReceiver {

  public HttpsMessageReceiver(Connector connector, FlowConstruct flow, InboundEndpoint endpoint) throws CreateException {
    super(connector, flow, endpoint);
  }

  @Override
  protected void doConnect() throws ConnectException {
    checkKeyStore();
    super.doConnect();
  }

  protected void checkKeyStore() throws ConnectException {
    HttpsConnector httpsConnector = (HttpsConnector) connector;
    String keyStore = httpsConnector.getKeyStore();
    if (StringUtils.isBlank(keyStore)) {
      throw new EndpointConnectException(CoreMessages.objectIsNull("tls-key-store"), this);
    }
  }

  @Override
  HttpMessageProcessTemplate createMessageProcessTemplate(HttpServerConnection httpServerConnection) {
    return new HttpsMessageProcessTemplate(this, httpServerConnection, getWorkManager());
  }
}
