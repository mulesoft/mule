/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.transport;

import static java.util.Collections.emptyMap;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import java.io.InputStream;

/**
 * Retrieve remote documents over HTTP using an Http Connector configuration.
 *
 * @since 4.0
 */
public final class HttpResourceLocator implements TransportResourceLocator {

  private final ReflectiveHttpConfigBasedRequester requester;

  public HttpResourceLocator(String configName, ExtensionsClient client) {
    this.requester = new ReflectiveHttpConfigBasedRequester(configName, client);
  }

  /**
   * {@inheritDoc}
   * <p>
   * handles `http` and `https` uris.
   */
  @Override
  public boolean handles(String url) {
    return url.startsWith("http");
  }

  /**
   * Retrieves the document's content over http.
   */
  @Override
  public InputStream getResource(String url) {
    return requester.get(url, emptyMap()).getFirst();
  }
}
