/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.provider;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.configuration.internal.export.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.info.ExecutionInitialSpanInfo;

import javax.inject.Inject;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.6.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider, Initialisable {

  public static final String API_ID_CONFIGURATION_PROPERTIES_KEY = "apiId";

  @Inject
  MuleContext muleContext;

  @Inject
  ConfigurationProperties configurationProperties;

  // TODO: User Story B - Implementation of Monitoring, Troubleshooting, App Level (W-12658074)
  private final InitialExportInfoProvider initialExportInfo = new MonitoringInitialExportInfoProvider();
  private String artifactTypeStringValue;
  private String artifactId;
  private String apiId;

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component) {
    return new ExecutionInitialSpanInfo(component, apiId, initialExportInfo);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    return new ExecutionInitialSpanInfo(component, apiId, initialExportInfo, null, suffix);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(String name) {
    return new ExecutionInitialSpanInfo(name, apiId, initialExportInfo);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
    return new ExecutionInitialSpanInfo(component, apiId, overriddenName, initialExportInfo);
  }

  @Override
  public void initialise() throws InitialisationException {
    this.artifactId = muleContext.getConfiguration().getId();
    ArtifactType artifactType = muleContext.getArtifactType();
    this.artifactTypeStringValue = artifactType.getAsString();

    if (artifactType.equals(POLICY)) {
      apiId = configurationProperties.resolveStringProperty(API_ID_CONFIGURATION_PROPERTIES_KEY).orElse(null);
    }

  }
}
