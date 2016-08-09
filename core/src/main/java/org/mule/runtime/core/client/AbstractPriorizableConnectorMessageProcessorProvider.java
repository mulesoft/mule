/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.client;

import org.mule.runtime.core.api.client.AbstractConnectorMessageProcessorProvider;
import org.mule.runtime.core.api.connector.ConnectorOperationProvider;

/**
 * Allows to define the order in which {@link ConnectorOperationProvider}s will be evaluated for use.
 */
public abstract class AbstractPriorizableConnectorMessageProcessorProvider extends AbstractConnectorMessageProcessorProvider {

  /**
   * Defines the priority in which different implementations of this interface will be evaluated to use by calling
   * {@link #supportsUrl(String)}.
   * <p>
   * This is useful when there are many possible providers for a same protocol, in order to have certainty as to which one of the
   * possible providers will be used.
   * <p>
   * instances with higher priority will be evaluated before the ones with lower priority.
   * 
   * @return the priority for this {@link ConnectorOperationProvider}.
   */
  public abstract int priority();
}
