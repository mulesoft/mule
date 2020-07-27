/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSessionBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

public class DefaultDeclarationSessionBuilder
    extends AbstractArtifactAgnosticServiceBuilder<DeclarationSessionBuilder, DeclarationSession>
    implements DeclarationSessionBuilder {

  public DefaultDeclarationSessionBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    super(defaultApplicationFactory);
  }

  @Override
  protected DeclarationSession createService(ApplicationSupplier applicationSupplier) {
    return new DefaultDeclarationSession(applicationSupplier);
  }

}
