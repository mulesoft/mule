/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.api.connection.ConnectionExceptionCode.UNKNOWN;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.module.tooling.api.artifact.TemporaryArtifact;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingObjectNotFoundException;
import org.mule.runtime.module.tooling.api.connectivity.UnsupportedConnectivityTestingObjectException;

import java.util.Collection;

/**
 * {@inheritDoc}
 */
public class DefaultConnectivityTestingService implements ConnectivityTestingService {

  private TemporaryArtifact temporaryArtifact;

  /**
   * Creates a {@code DefaultConnectivityTestingService}.
   *
   * @param temporaryArtifact tooling artifact used to do connectivity testing
   */
  public DefaultConnectivityTestingService(TemporaryArtifact temporaryArtifact) {
    this.temporaryArtifact = temporaryArtifact;
  }

  /**
   * {@inheritDoc}
   *
   * @throws MuleRuntimeException
   */
  @Override
  public ConnectionValidationResult testConnection(String identifier) {
    checkArgument(identifier != null, "identifier cannot be null");
    if (!temporaryArtifact.isStarted()) {
      try {
        temporaryArtifact.start();
      } catch (InitialisationException e) {
        return failure(e.getMessage(), UNKNOWN, e);
      } catch (ConfigurationException e) {
        return failure(e.getMessage(), UNKNOWN, e);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
    Object connectivityTestingObject = temporaryArtifact.getMuleContext().getRegistry().get(identifier);
    if (connectivityTestingObject == null) {
      throw new ConnectivityTestingObjectNotFoundException(createStaticMessage("It was not possible to find an object to do connectivity testing"));
    }
    Collection<ConnectivityTestingStrategy> connectivityTestingStrategies =
        temporaryArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class);
    for (ConnectivityTestingStrategy connectivityTestingStrategy : connectivityTestingStrategies) {
      if (connectivityTestingStrategy.accepts(connectivityTestingObject)) {
        try {
          return connectivityTestingStrategy.testConnectivity(connectivityTestingObject);
        } catch (Exception e) {
          return failure(e.getMessage(), UNKNOWN, e);
        }
      }
    }
    throw new UnsupportedConnectivityTestingObjectException(createStaticMessage("Could not do connectivity testing over object of type "
        + connectivityTestingObject.getClass().getName()));
  }
}
