/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.mbean;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MBean for application-wide statistics
 */
public class ApplicationService extends FlowConstructService implements FlowConstructServiceMBean {

  private static Logger LOGGER = LoggerFactory.getLogger(ApplicationService.class);

  public ApplicationService(String type, String name, MuleContext muleContext, FlowConstructStatistics statistics) {
    super(type, name, muleContext, statistics);
  }

  @Override
  public void postRegister(Boolean registrationDone) {
    try {
      statsName = jmxSupport.getObjectName(String.format("%s:type=org.mule.Statistics,%s=%s", objectName.getDomain(),
                                                         statistics.getFlowConstructType(), jmxSupport.escape(getName())));

      // unregister old version if exists
      if (this.server.isRegistered(statsName)) {
        this.server.unregisterMBean(statsName);
      }

      this.server.registerMBean(new FlowConstructStats(statistics), this.statsName);
    } catch (Exception e) {
      LOGGER.error("Error post-registering the MBean", e);
    }
  }
}
