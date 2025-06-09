/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_FORCE_TOOLING_APP_LOGS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.tooling.internal.config.ToolingServicesConfigurator.SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;

import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSessionBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

import java.io.File;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DefaultDeclarationSessionBuilder
    extends AbstractArtifactAgnosticServiceBuilder<DeclarationSessionBuilder, DeclarationSession>
    implements DeclarationSessionBuilder {

  private static final String TRUE = "true";
  private static final String FALSE = "false";

  // System Property to allow disable cache storage for Metadata resolution on Runtime side.
  public static final String MULE_METADATA_CACHE_DISABLE = "mule.metadata.cache.disabled";

  public DefaultDeclarationSessionBuilder(DefaultApplicationFactory defaultApplicationFactory) {
    super(defaultApplicationFactory);
  }

  @Override
  protected Map<String, String> forcedDeploymentProperties() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
        // System Property for user allow to force enable logs, but internal property is meant to disable logs if it is true
        .put(MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY,
             String.valueOf(!valueOf(getProperty(MULE_FORCE_TOOLING_APP_LOGS_DEPLOYMENT_PROPERTY, FALSE))))
        .put(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, TRUE)
        .put(MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY, TRUE);
    if (!valueOf(getProperty(MULE_METADATA_CACHE_DISABLE, FALSE))) {
      // Setting the deployment property that enables the shared persistent for Metadata cache using OS.
      builder.put(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH, getToolingWorkingDir().getAbsolutePath());
    }

    return builder.build();
  }

  private File getToolingWorkingDir() {
    return new File(getExecutionFolder(), "tooling");
  }

  @Override
  protected DeclarationSession createService(ApplicationSupplier applicationSupplier) {
    return new DefaultDeclarationSession(applicationSupplier);
  }

}
