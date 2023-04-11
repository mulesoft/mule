/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.provider;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.info.ExecutionInitialSpanInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import javax.inject.Inject;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.6.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider {

  public static final String API_ID_CONFIGURATION_PROPERTIES_KEY = "apiId";

  @Inject
  MuleContext muleContext;

  @Inject
  ConfigurationProperties configurationProperties;

  @Inject
  TracingLevelConfiguration tracingLevelConfiguration;

  // TODO: User Story B - Implementation of Monitoring, Troubleshooting, App Level (W-12658074)
  private InitialExportInfoProvider initialExportInfoProvider;
  private String apiId;
  private boolean initialisedAttributes;

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component) {
    // TODO: Verify initialisation order in mule context (W-12761329)
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    return new ExecutionInitialSpanInfo(component, apiId, getInitialExportInfoProvider());
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    return new ExecutionInitialSpanInfo(component, apiId, getInitialExportInfoProvider(), null, suffix);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    return new ExecutionInitialSpanInfo(component, apiId, overriddenName, getInitialExportInfoProvider());
  }

  public void initialiseAttributes() {
    if (muleContext.getArtifactType().equals(POLICY)) {
      apiId = configurationProperties.resolveStringProperty(API_ID_CONFIGURATION_PROPERTIES_KEY).orElse(null);
    }
  }

  private InitialExportInfoProvider getInitialExportInfoProvider() {
    if (initialExportInfoProvider == null) {
      TracingLevel tracingLevel = tracingLevelConfiguration.getTracingLevel();
      resolveInitialExportInfoProvider(tracingLevel);
    }

    return initialExportInfoProvider;
  }

  private void resolveInitialExportInfoProvider(TracingLevel tracingLevel) {
    switch (tracingLevel) {
      case OVERVIEW:
        initialExportInfoProvider = new OverviewInitialExportInfoProvider();
        break;
      case DEBUG:
        initialExportInfoProvider = new DebugInitialExportInfoProvider();
        break;
      default:
        initialExportInfoProvider = new MonitoringInitialExportInfoProvider();
    }
  }
}
