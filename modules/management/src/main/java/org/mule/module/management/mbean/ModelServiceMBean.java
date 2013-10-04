/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;

/**
 * <code>ModelServiceMBean</code> JMX Service interface for the Model.
 */
public interface ModelServiceMBean
{
    String DEFAULT_JMX_NAME_PREFIX = "type=Model,name=";

    void start() throws MuleException;

    void stop() throws MuleException;

//    boolean isComponentRegistered(String name);
//
//    UMODescriptor getComponentDescriptor(String name);
//
//    void startComponent(String name) throws MuleException;
//
//    void stopComponent(String name) throws MuleException;
//
//    void pauseComponent(String name) throws MuleException;
//
//    void resumeComponent(String name) throws MuleException;
//
//    void unregisterComponent(String name) throws MuleException;

    String getName();

    String getType();
}
