/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.agent;

import org.mule.runtime.core.api.MuleException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

/**
 * JMX Agent for Mule Applications
 */
public class JmxApplicationAgent extends AbstractJmxAgent
{

    @Override
    protected void registerServices() throws MuleException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        registerWrapperService();
        registerStatisticsService();
        registerMuleService();
        registerConfigurationService();
        registerFlowConstructServices();
        registerEndpointServices();
        registerConnectorServices();
        registerApplicationServices();
    }

}
