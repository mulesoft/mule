/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_FORCE_TOOLING_APP_LOGS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSessionBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class DefaultDeclarationSessionBuilder
    extends AbstractArtifactAgnosticServiceBuilder<DeclarationSessionBuilder, DeclarationSession>
    implements DeclarationSessionBuilder {

  private static final String TRUE = "true";

  public DefaultDeclarationSessionBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    super(defaultApplicationFactory);
  }

  @Override
  protected Map<String, String> forcedDeploymentProperties() {
    return ImmutableMap.<String, String>builder()
        // System Property for user allow to force enable logs, but internal property is meant to disable logs if it is true
        .put(MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY,
             String.valueOf(!valueOf(getProperty(MULE_FORCE_TOOLING_APP_LOGS_DEPLOYMENT_PROPERTY, "false"))))
        .put(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, TRUE)
        .put(MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY, TRUE)
        .build();
  }

  @Override
  protected DeclarationSession createService(ApplicationSupplier applicationSupplier) {
    return new DefaultDeclarationSession(applicationSupplier);
  }

}
