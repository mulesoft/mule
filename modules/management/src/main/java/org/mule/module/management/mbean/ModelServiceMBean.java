/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
