/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.compatibility.transport.http.HttpsConnector.LOCAL_CERTIFICATES;
import static org.mule.compatibility.transport.http.HttpsConnector.PEER_CERTIFICATES;

import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.context.WorkManager;

public class HttpsMessageProcessTemplate extends HttpMessageProcessTemplate {

  public HttpsMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection,
                                     final WorkManager flowExecutionWorkManager) {
    super(messageReceiver, httpServerConnection);
  }

  @Override
  public Event beforeRouteEvent(Event muleEvent) throws MuleException {
    try {
      long timeout = ((HttpsConnector) getConnector()).getSslHandshakeTimeout();
      boolean handshakeComplete = getHttpServerConnection().getSslSocketHandshakeCompleteLatch().await(timeout, MILLISECONDS);
      if (!handshakeComplete) {
        throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(), muleEvent);
      }
    } catch (InterruptedException e) {
      throw new MessagingException(HttpMessages.sslHandshakeDidNotComplete(), muleEvent, e);
    }
    InternalMessage.Builder messageBuilder = InternalMessage.builder(muleEvent.getMessage());
    if (getHttpServerConnection().getPeerCertificateChain() != null) {
      messageBuilder.addOutboundProperty(PEER_CERTIFICATES, getHttpServerConnection().getPeerCertificateChain());
    }
    if (getHttpServerConnection().getLocalCertificateChain() != null) {
      messageBuilder.addOutboundProperty(LOCAL_CERTIFICATES, getHttpServerConnection().getLocalCertificateChain());
    }
    muleEvent = Event.builder(muleEvent).message(messageBuilder.build()).build();

    super.beforeRouteEvent(muleEvent);
    return muleEvent;
  }
}
