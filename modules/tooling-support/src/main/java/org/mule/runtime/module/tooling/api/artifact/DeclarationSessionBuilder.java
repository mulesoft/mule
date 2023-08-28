/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.api.artifact;

import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;

/**
 * Provides all required steps to configure and build a new {@link DeclarationSession}
 *
 * @since 4.4.0
 */
public interface DeclarationSessionBuilder
    extends ArtifactAgnosticServiceBuilder<DeclarationSessionBuilder, DeclarationSession> {

}
