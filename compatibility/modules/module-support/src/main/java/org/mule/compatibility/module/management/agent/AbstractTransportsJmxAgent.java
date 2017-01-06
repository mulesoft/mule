/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.management.agent;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.compatibility.module.management.agent.mbean.ConnectorService;
import org.mule.compatibility.module.management.agent.mbean.ConnectorServiceMBean;
import org.mule.compatibility.module.management.agent.mbean.EndpointService;
import org.mule.compatibility.module.management.agent.mbean.EndpointServiceMBean;
import org.mule.runtime.module.management.agent.AbstractJmxAgent;
import org.mule.runtime.module.management.agent.ClassloaderSwitchingMBeanWrapper;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public abstract class AbstractTransportsJmxAgent extends AbstractJmxAgent {

  protected void registerEndpointServices() throws NotCompliantMBeanException, MBeanRegistrationException,
      InstanceAlreadyExistsException, MalformedObjectNameException {
    for (Connector connector : muleContext.getRegistry().lookupObjects(Connector.class)) {
      if (connector instanceof AbstractConnector) {
        for (MessageReceiver messageReceiver : ((AbstractConnector) connector).getReceivers().values()) {
          if (muleContext.equals(messageReceiver.getFlowConstruct().getMuleContext())) {
            EndpointServiceMBean service = new EndpointService(messageReceiver);

            String fullName = buildFullyQualifiedEndpointName(service, connector);
            if (logger.isInfoEnabled()) {
              logger.info("Attempting to register service with name: " + fullName);
            }

            ObjectName on = jmxSupport.getObjectName(fullName);
            ClassloaderSwitchingMBeanWrapper mBean =
                new ClassloaderSwitchingMBeanWrapper(service, EndpointServiceMBean.class, muleContext.getExecutionClassLoader());
            getMBeanServer().registerMBean(mBean, on);
            if (logger.isInfoEnabled()) {
              logger.info("Registered Endpoint Service with name: " + on);
            }
          }
        }
      } else {
        logger.warn("Connector: " + connector + " is not an istance of AbstractConnector, cannot obtain Endpoint MBeans from it");
      }
    }
  }

  protected String buildFullyQualifiedEndpointName(EndpointServiceMBean mBean, Connector connector) {
    String rawName = jmxSupport.escape(mBean.getName());

    StringBuilder fullName = new StringBuilder(128);
    fullName.append(jmxSupport.getDomainName(muleContext, !containerMode));
    fullName.append(":type=Endpoint,service=");
    fullName.append(jmxSupport.escape(mBean.getComponentName()));
    fullName.append(",connector=");
    fullName.append(connector.getName());
    fullName.append(",name=");
    fullName.append(rawName);
    return fullName.toString();
  }

  protected void registerConnectorServices() throws MalformedObjectNameException, NotCompliantMBeanException,
      MBeanRegistrationException, InstanceAlreadyExistsException {
    for (Connector connector : muleContext.getRegistry().lookupLocalObjects(Connector.class)) {
      ConnectorServiceMBean service = new ConnectorService(connector);
      final String rawName = service.getName();
      final String name = jmxSupport.escape(rawName);
      final String jmxName = String.format("%s:%s%s", jmxSupport.getDomainName(muleContext, !containerMode),
                                           ConnectorServiceMBean.DEFAULT_JMX_NAME_PREFIX, name);
      if (logger.isDebugEnabled()) {
        logger.debug("Attempting to register service with name: " + jmxName);
      }
      ObjectName oName = jmxSupport.getObjectName(jmxName);
      ClassloaderSwitchingMBeanWrapper mBean =
          new ClassloaderSwitchingMBeanWrapper(service, ConnectorServiceMBean.class, muleContext.getExecutionClassLoader());
      getMBeanServer().registerMBean(mBean, oName);
      logger.info("Registered Connector Service with name " + oName);
    }
  }


}
