/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport.service;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupConnector;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.registerConnector;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.registry.LegacyServiceType;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.compatibility.core.util.TransportObjectNameHelper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.util.BeanUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TransportFactory</code> can be used for generically creating endpoints from an url. Note that for some endpoints, the url
 * alone is not enough to create the endpoint if a connector for the endpoint has not already been configured with the Mule
 * Manager.
 */
public class TransportFactory {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(TransportFactory.class);

  protected MuleContext muleContext;

  public TransportFactory(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Creates an uninitialied connector from the provided MuleEndpointURI. The scheme is used to determine what kind of connector
   * to create. Any params set on the uri can be used to initialise bean properties on the created connector.
   * <p/>
   * Note that the initalise method will need to be called on the connector returned. This is so that developers can control when
   * the connector initialisation takes place as this is likely to initialse all connecotr resources.
   *
   * @param url the MuleEndpointURI url to create the connector with
   * @return a new Connector
   * @throws TransportFactoryException
   */
  public Connector createConnector(EndpointURI url) throws TransportFactoryException {

    try {
      Connector connector;
      String scheme = url.getFullScheme();

      TransportServiceDescriptor sd =
          (TransportServiceDescriptor) lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT, scheme,
                                                               null);
      if (sd == null) {
        throw new ServiceException(TransportCoreMessages.noServiceTransportDescriptor(scheme));
      }

      connector = sd.createConnector();
      if (connector != null) {
        if (connector instanceof AbstractConnector) {
          ((AbstractConnector) connector).initialiseFromUrl(url);
        }
      } else {
        throw new TransportFactoryException(TransportCoreMessages.objectNotSetInService("Connector", scheme));
      }

      connector.setName(new TransportObjectNameHelper(muleContext).getConnectorName(connector));

      return connector;
    } catch (Exception e) {
      throw new TransportFactoryException(TransportCoreMessages.failedToCreateObjectWith("Endpoint", url), e);
    }
  }

  public Connector createConnector(String uri) throws TransportFactoryException {
    try {
      return createConnector(new MuleEndpointURI(uri, muleContext));
    } catch (EndpointException e) {
      throw new TransportFactoryException(e);
    }
  }

  public Connector getOrCreateConnectorByProtocol(ImmutableEndpoint endpoint) throws TransportFactoryException {
    return getOrCreateConnectorByProtocol(endpoint.getEndpointURI());
  }

  /**
   * Returns an initialized connector.
   */
  public Connector getOrCreateConnectorByProtocol(EndpointURI uri) throws TransportFactoryException {
    String connectorName = uri.getConnectorName();
    if (null != connectorName) {
      // TODO this lookup fails currently on Mule 2.x! MuleAdminAgentTestCase
      Connector connector = lookupConnector(muleContext.getRegistry(), connectorName);
      if (connector != null) {
        return connector;
      }
    }

    Connector connector = getConnectorByProtocol(uri.getFullScheme());
    if (connector == null) {
      connector = createConnector(uri);
      try {
        BeanUtils.populate(connector, uri.getParams());
        registerConnector(muleContext.getRegistry(), connector);
      } catch (Exception e) {
        throw new TransportFactoryException(e);
      }
    }
    return connector;
  }

  public Connector getConnectorByProtocol(String protocol) {
    Connector connector;
    List<Connector> results = new ArrayList<Connector>();
    Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
    for (Iterator iterator = connectors.iterator(); iterator.hasNext();) {
      connector = (Connector) iterator.next();
      if (connector.supportsProtocol(protocol)) {
        results.add(connector);
      }
    }
    if (results.size() > 1) {
      StringBuilder buf = new StringBuilder();
      for (Connector result : results) {
        buf.append(result.getName()).append(", ");
      }
      throw new IllegalStateException(TransportCoreMessages.moreThanOneConnectorWithProtocol(protocol, buf.toString())
          .getMessage());
    } else if (results.size() == 1) {
      return results.get(0);
    } else {
      return null;
    }
  }

  public Connector getDefaultConnectorByProtocol(String protocol) {
    Connector connector;
    List<Connector> results = new ArrayList<Connector>();
    Collection connectors = muleContext.getRegistry().lookupObjects(Connector.class);
    for (Iterator iterator = connectors.iterator(); iterator.hasNext();) {
      connector = (Connector) iterator.next();
      if (connector.supportsProtocol(protocol) && TransportObjectNameHelper.isDefaultAutoGeneratedConnector(connector)) {
        results.add(connector);
      }
    }
    if (results.size() > 1) {
      StringBuilder buf = new StringBuilder();
      for (Connector result : results) {
        buf.append(result.getName()).append(", ");
      }
      throw new IllegalStateException(TransportCoreMessages.moreThanOneConnectorWithProtocol(protocol, buf.toString())
          .getMessage());
    } else if (results.size() == 1) {
      return results.get(0);
    } else {
      return null;
    }
  }
}
