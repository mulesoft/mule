/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;

/**
 * Builder for creating a {@code ConnectivityTestingService} from a set of extensions and an {@code ArtifactConfiguration} that
 * describes a set of mule components.
 *
 * @since 4.0
 */
@NoImplement
public interface ConnectivityTestingServiceBuilder
    extends ArtifactAgnosticServiceBuilder<ConnectivityTestingServiceBuilder, ConnectivityTestingService> {

}
