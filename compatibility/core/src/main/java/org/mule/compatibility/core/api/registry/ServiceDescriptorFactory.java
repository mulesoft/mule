/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.registry;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.compatibility.core.transport.service.DefaultTransportServiceDescriptor;
import org.mule.compatibility.core.transport.service.MetaTransportServiceDescriptor;
import org.mule.compatibility.core.transport.service.TransportServiceDescriptor;
import org.mule.compatibility.core.util.SpiUtils;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory used to create a new service descriptor.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public class ServiceDescriptorFactory {

  protected static Logger logger = LoggerFactory.getLogger(ServiceDescriptorFactory.class);

  /**
   * Factory method to create a new service descriptor.
   *
   * @param type the service type to create
   * @param name the name of the service. In the case of a stransport service, the full endpoint sheme should be used here i.e.
   *        'cxf:http'
   * @param props The properties defined by this service type
   * @param overrides any overrides that should be configured on top of the standard propertiers for the service
   * @param muleContext the MuleContext for this mule instance
   * @param classLoader the ClassLoader to use when loading classes
   * @return a ServiceDescriptor instance that can be used to create the service objects associated with the service name
   * @throws ServiceException if the service cannot be located
   */
  public static ServiceDescriptor create(ServiceType type, String name, Properties props, Properties overrides,
                                         MuleContext muleContext, ClassLoader classLoader)
      throws ServiceException {
    if (overrides != null) {
      props.putAll(overrides);
    }

    String scheme = name;
    String metaScheme = null;
    int i = name.indexOf(":");
    if (i > -1) {
      scheme = name.substring(i + 1);
      metaScheme = name.substring(0, i);
    }
    // TODO we currently need to filter out transports that implement the meta scheme the old way
    if (isFilteredMetaScheme(metaScheme)) {
      // handle things the old way for now
      metaScheme = null;
    } else if (name.startsWith("jetty:http")) {
      scheme = "jetty";
    }

    String serviceFinderClass = (String) props.remove(MuleProperties.SERVICE_FINDER);

    ServiceDescriptor sd;
    if (type.equals(LegacyServiceType.TRANSPORT)) {
      try {
        if (metaScheme != null) {
          sd = new MetaTransportServiceDescriptor(metaScheme, scheme, props, classLoader);
        } else {
          sd = new DefaultTransportServiceDescriptor(scheme, props, classLoader);
        }
      } catch (ServiceException e) {
        throw e;
      } catch (Exception e) {
        throw new ServiceException(CoreMessages.failedToCreate("Transport: " + name));
      }
      Properties exceptionMappingProps = SpiUtils.findServiceDescriptor(ServiceType.EXCEPTION, name + "-exception-mappings");
      ((TransportServiceDescriptor) sd).setExceptionMappings(exceptionMappingProps);
    } else {
      throw new ServiceException(TransportCoreMessages.unrecognisedServiceType(type));
    }

    // If there is a finder service, use it to find the "real" service.
    if (StringUtils.isNotBlank(serviceFinderClass)) {
      ServiceFinder finder;
      try {
        finder = (ServiceFinder) ClassUtils.instanciateClass(serviceFinderClass);
      } catch (Exception e) {
        throw new ServiceException(TransportCoreMessages.cannotInstanciateFinder(serviceFinderClass), e);
      }
      String realService = finder.findService(name, sd, props);
      if (realService != null) {
        // Recursively look up the service descriptor for the real service.
        return lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT, realService, overrides);
      } else {
        throw new ServiceException(TransportCoreMessages.serviceFinderCantFindService(name));
      }
    }
    return sd;
  }

  protected static boolean isFilteredMetaScheme(String metaScheme) {
    if ("axis".equals(metaScheme) || "wsdl-axis".equals(metaScheme) || "cxf".equals(metaScheme) || "wsdl-cxf".equals(metaScheme)
        || "jms".equals(metaScheme) || "wmq".equals(metaScheme) || "ajax".equals(metaScheme)) {
      return true;
    }
    return false;
  }
}


