/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.BeanUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * A base class used for Meta endpoint builders such as RSS or ATOM. This class overrides the
 * {@link #setProperties(java.util.Map)} method
 */
public abstract class AbstractMetaEndpointBuilder extends EndpointURIEndpointBuilder {

  protected AbstractMetaEndpointBuilder() {}

  protected AbstractMetaEndpointBuilder(EndpointURIEndpointBuilder global) throws EndpointException {
    super(global);
  }

  protected AbstractMetaEndpointBuilder(URIBuilder builder) {
    super(builder);
  }

  protected AbstractMetaEndpointBuilder(String address, MuleContext muleContext) {
    super(address, muleContext);
  }

  protected AbstractMetaEndpointBuilder(EndpointURI endpointURI) {
    super(endpointURI);
  }

  protected AbstractMetaEndpointBuilder(ImmutableEndpoint source) {
    super(source);
  }

  @Override
  public void setProperties(Map<String, Serializable> properties) {
    // This is required since properties were historically set as a properties map
    for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
      try {
        BeanUtils.setProperty(this, entry.getKey().toString(), entry.getValue());
      } catch (Exception e) {
        // ignore
      }
    }
    properties.remove("connector");
    super.setProperties(properties);
  }

  @Override
  protected String getScheme() {
    return uriBuilder.getEndpoint().getScheme();
  }

  public static String getEndpointAddressWithoutMetaScheme(String string) {
    int idx = string.indexOf(':');
    if (idx != -1) {
      string = string.substring(idx + 1);
    }
    return string;
  }

  @Override
  protected Connector getConnector() throws EndpointException {
    AbstractConnector c = (AbstractConnector) super.getConnector();
    EndpointURI endpointURI = uriBuilder.getEndpoint();
    if (!c.supportsProtocol(endpointURI.getFullScheme())) {
      c.registerSupportedMetaProtocol(endpointURI.getSchemeMetaInfo());
    }
    return c;
  }
}
