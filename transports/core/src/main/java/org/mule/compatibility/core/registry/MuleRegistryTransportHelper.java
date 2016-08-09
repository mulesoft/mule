/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.registry;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.registry.AbstractServiceDescriptor;
import org.mule.compatibility.core.api.registry.ServiceDescriptor;
import org.mule.compatibility.core.api.registry.ServiceDescriptorFactory;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.util.SpiUtils;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.registry.MuleRegistryHelper;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class MuleRegistryTransportHelper {

  protected static Logger logger = LoggerFactory.getLogger(MuleRegistryTransportHelper.class);

  public static Connector lookupConnector(MuleRegistry registry, String name) {
    return (Connector) registry.lookupObject(name);
  }

  /**
   * Looks-up endpoint builders which can be used to repeatably create endpoints with the same configuration. These endpoint
   * builder are either global endpoints or they are builders used to create named endpoints configured on routers and exception
   * strategies.
   *
   * @param name the name of the endpointBuilder to find
   * @return An endpointBuilder with the name specified or null if there is no endpoint builder with that name
   */
  public static EndpointBuilder lookupEndpointBuilder(MuleRegistry registry, String name) {
    Object o = registry.lookupObject(name);
    if (o instanceof EndpointBuilder) {
      if (logger.isDebugEnabled()) {
        logger.debug("Global endpoint EndpointBuilder for name: " + name + " found");
      }
      return (EndpointBuilder) o;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("No endpoint builder with the name: " + name + " found.");
      }
      return null;
    }
  }

  public static void registerConnector(MuleRegistry registry, Connector connector) throws MuleException {
    registry.registerObject(((MuleRegistryHelper) registry).getName(connector), connector, Connector.class);
  }

  // TODO MULE-2494
  public static void registerEndpoint(MuleRegistry registry, ImmutableEndpoint endpoint) throws MuleException {
    registry.registerObject(((MuleRegistryHelper) registry).getName(endpoint), endpoint, ImmutableEndpoint.class);
  }

  public static void registerEndpointBuilder(MuleRegistry registry, String name, EndpointBuilder builder) throws MuleException {
    registry.registerObject(name, builder, EndpointBuilder.class);
  }

  /**
   * Looks up the service descriptor from a singleton cache and creates a new one if not found.
   */
  public static ServiceDescriptor lookupServiceDescriptor(MuleRegistry registry, ServiceType type, String name,
                                                          Properties overrides)
      throws ServiceException {
    String key = new AbstractServiceDescriptor.Key(name, overrides).getKey();
    // TODO If we want these descriptors loaded form Spring we need to change the key mechanism
    // and the scope, and then deal with circular reference issues.

    synchronized (registry) {
      ServiceDescriptor sd = registry.lookupObject(key);
      if (sd == null) {
        sd = createServiceDescriptor(registry, type, name, overrides);
        try {
          registry.registerObject(key, sd, ServiceDescriptor.class);
        } catch (RegistrationException e) {
          throw new ServiceException(e.getI18nMessage(), e);
        }
      }
      return sd;
    }
  }

  protected static ServiceDescriptor createServiceDescriptor(MuleRegistry registry, ServiceType type, String name,
                                                             Properties overrides)
      throws ServiceException {
    // Stripe off and use the meta-scheme if present
    String scheme = name;
    if (name.contains(":")) {
      scheme = name.substring(0, name.indexOf(":"));
    }

    Properties props = SpiUtils.findServiceDescriptor(type, scheme);
    if (props == null) {
      throw new ServiceException(CoreMessages.failedToLoad(type + " " + scheme));
    }

    return ServiceDescriptorFactory.create(type, name, props, overrides, ((MuleRegistryHelper) registry).getMuleContext(),
                                           ((MuleRegistryHelper) registry).getMuleContext().getExecutionClassLoader());
  }


}
