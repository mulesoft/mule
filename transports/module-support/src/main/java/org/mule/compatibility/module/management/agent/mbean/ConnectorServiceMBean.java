/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.management.agent.mbean;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Initialisable;

public interface ConnectorServiceMBean extends Initialisable {

  String DEFAULT_JMX_NAME_PREFIX = "type=Connector,name=";

  boolean isStarted();

  boolean isDisposed();

  String getName();

  String getProtocol();

  void startConnector() throws MuleException;

  void stopConnector() throws MuleException;

  void dispose();


}
