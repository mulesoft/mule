/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilderTestCase;

import io.qameta.allure.Issue;

@Issue("W-13057814")
public class DefaultDeclarationSessionBuilderTestCase extends AbstractArtifactAgnosticServiceBuilderTestCase {

  @Override
  protected ArtifactAgnosticServiceBuilder getArtifactAgnosticServiceBuilder(DefaultApplicationFactory applicationFactory) {
    return new DefaultDeclarationSessionBuilder(applicationFactory);
  }

}
