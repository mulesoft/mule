/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
