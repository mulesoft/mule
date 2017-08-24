/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message.dispatcher;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.message.DispatchingRequest;
import org.mule.runtime.extension.api.soap.message.DispatchingResponse;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.transport.ReflectiveHttpConfigBasedRequester;
import java.io.InputStream;
import java.util.Map;

/**
 * A {@link MessageDispatcher} that dispatches the SOAP request via HTTP using an HTTP connector provided configuration.
 * <p>
 * As the this lives in mule and it cannot depend on the HTTP extension, reflection is used to access the returned headers.
 *
 * @since 4.0
 */
public final class HttpConfigBasedMessageDispatcher implements MessageDispatcher {

  private final ReflectiveHttpConfigBasedRequester requester;

  public HttpConfigBasedMessageDispatcher(String configName, ExtensionsClient client) {
    this.requester = new ReflectiveHttpConfigBasedRequester(configName, client);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Dispatches the message using the {@link ExtensionsClient} executing the {@code request} operation of the HTTP extension.
   * <p>
   */
  @Override
  public DispatchingResponse dispatch(DispatchingRequest req) {
    Pair<InputStream, Map<String, String>> result = requester.post(req.getAddress(), req.getHeaders(), req.getContent());
    return new DispatchingResponse(result.getFirst(), result.getSecond());
  }
}
