/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;

public interface ConnectorServiceMBean extends Initialisable
{
    String DEFAULT_JMX_NAME_PREFIX = "type=Connector,name=";

    boolean isStarted();

    boolean isDisposed();

    String getName();

    String getProtocol();

    void startConnector() throws MuleException;

    void stopConnector() throws MuleException;

    void dispose();


}
