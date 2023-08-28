/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal.connectivity;

import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

/**
 * Default implementation for {@code ConnectivityTestingServiceBuilder}.
 *
 * @since 4.0
 */
public class DefaultConnectivityTestingServiceBuilder
    extends AbstractArtifactAgnosticServiceBuilder<ConnectivityTestingServiceBuilder, ConnectivityTestingService>
    implements ConnectivityTestingServiceBuilder {

  public DefaultConnectivityTestingServiceBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    super(defaultApplicationFactory);
  }

  @Override
  protected ConnectivityTestingService createService(ApplicationSupplier applicationSupplier) {
    return new TemporaryArtifactConnectivityTestingService(applicationSupplier);
  }

}
