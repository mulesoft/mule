/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.connector;

import static java.util.Collections.sort;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.connector.ConnectorOperationProvider;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.client.AbstractPriorizableConnectorMessageProcessorProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default {@link org.mule.runtime.core.api.connector.ConnectorOperationLocator} that will search in the mule registry for
 * registered {@link org.mule.runtime.core.api.connector.ConnectorOperationLocator} to later provider operations through the use
 * of URLs.
 */
public class MuleConnectorOperationLocator implements ConnectorOperationLocator, MuleContextAware, Initialisable {

  private MuleContext muleContext;
  private Collection<ConnectorOperationProvider> connectorOperationProviders;

  @Override
  public void initialise() throws InitialisationException {
    final List<ConnectorOperationProvider> providers =
        new ArrayList<>(muleContext.getRegistry().lookupObjects(ConnectorOperationProvider.class));
    sort(providers, ((ConnectorOperationProvider p1, ConnectorOperationProvider p2) -> priority(p2) - priority(p1)));

    this.connectorOperationProviders = providers;
  }

  private int priority(ConnectorOperationProvider provider) {
    if (provider instanceof AbstractPriorizableConnectorMessageProcessorProvider) {
      return ((AbstractPriorizableConnectorMessageProcessorProvider) provider).priority();
    } else {
      return 0;
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public MessageProcessor locateConnectorOperation(String url, OperationOptions operationOptions,
                                                   MessageExchangePattern exchangePattern)
      throws MuleException {
    for (ConnectorOperationProvider connectorOperationProvider : connectorOperationProviders) {
      if (connectorOperationProvider.supportsUrl(url)) {
        return connectorOperationProvider.getMessageProcessor(url, operationOptions, exchangePattern);
      }
    }
    return null;
  }

}
