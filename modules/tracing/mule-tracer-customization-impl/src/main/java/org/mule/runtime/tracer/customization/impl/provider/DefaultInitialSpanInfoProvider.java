/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getLocationAsString;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.info.ExecutionInitialSpanInfo;
import org.mule.runtime.tracer.customization.impl.export.TracingLevelExportInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import javax.inject.Inject;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.5.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider {

  public static final String API_ID_CONFIGURATION_PROPERTIES_KEY = "apiId";

  @Inject
  MuleContext muleContext;

  @Inject
  ConfigurationProperties configurationProperties;

  @Inject
  TracingLevelConfiguration tracingLevelConfiguration;

  private String apiId;
  private boolean initialisedAttributes;

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component) {
    // TODO: Verify initialisation order in mule context (W-12761329)
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    String location = getLocationAsString(component.getLocation());
    return new ExecutionInitialSpanInfo(component, apiId, getTracingLevelExportInfo(location));
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    String location = getLocationAsString(component.getLocation());
    return new ExecutionInitialSpanInfo(component, apiId, getTracingLevelExportInfo(location), null, suffix);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
    // TODO: Verify initialisation order in mule context (W-12761329). General registry problem
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }
    String location = getLocationAsString(component.getLocation());
    return new ExecutionInitialSpanInfo(component, apiId, overriddenName, getTracingLevelExportInfo(location));
  }

  public void initialiseAttributes() {
    if (muleContext.getArtifactType().equals(POLICY)) {
      apiId = configurationProperties.resolveStringProperty(API_ID_CONFIGURATION_PROPERTIES_KEY).orElse(null);
    }
  }

  private TracingLevelExportInfo getTracingLevelExportInfo(String location) {
    TracingLevel tracingLevel = tracingLevelConfiguration.getTracingLevel();
    TracingLevel tracingLevelOverride = tracingLevelConfiguration.getTracingLevelOverride(location);
    if (!tracingLevelOverride.equals(tracingLevel)) {
      return new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevelOverride), true);
    }
    return new TracingLevelExportInfo(resolveInitialExportInfoProvider(tracingLevel), false);
  }

  private InitialExportInfoProvider resolveInitialExportInfoProvider(TracingLevel tracingLevel) {
    switch (tracingLevel) {
      case OVERVIEW:
        return new OverviewInitialExportInfoProvider();
      case DEBUG:
        return new DebugInitialExportInfoProvider();
      default:
        return new MonitoringInitialExportInfoProvider();
    }
  }
}
