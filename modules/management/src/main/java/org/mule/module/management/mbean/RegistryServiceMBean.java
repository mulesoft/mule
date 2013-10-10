/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;

/**
 * <code>RegistryServiceMBean</code> JMX Service interface for the Registry
 */
public interface RegistryServiceMBean
{

    void start() throws MuleException;

    void stop() throws MuleException;

    //String getPersistenceMode();

    String getName();

}
