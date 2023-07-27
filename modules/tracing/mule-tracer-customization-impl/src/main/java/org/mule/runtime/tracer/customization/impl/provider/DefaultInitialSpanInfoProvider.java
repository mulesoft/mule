/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.customization.impl.info.ExecutionInitialSpanInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.5.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider {

  private Map<InitialSpanInfoIdentifier, ExecutionInitialSpanInfo> componentInitialSpanInfos = new ConcurrentHashMap<>();
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
    return new LazyInitialSpanInfo(() -> doGetInitialSpanInfo(component, null, null));
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    return new LazyInitialSpanInfo(() -> doGetInitialSpanInfo(component, suffix, null));
  }


  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
    return new LazyInitialSpanInfo(() -> doGetInitialSpanInfo(component, suffix, overriddenName));
  }

  private InitialSpanInfo doGetInitialSpanInfo(Component component, String suffix, String overriddenName) {
    // TODO: Verify initialisation order in mule context (W-12761329)
    if (!initialisedAttributes) {
      initialiseAttributes();
      initialisedAttributes = true;
    }

    // TODO: when to change the tracing level from other criteria than location is required, we will probably have to see this.
    if (component.getLocation() == null) {
      return new ExecutionInitialSpanInfo(component, apiId, overriddenName, suffix,
                                          tracingLevelConfiguration);
    }

    ExecutionInitialSpanInfo executionInitialSpanInfo =
        componentInitialSpanInfos.computeIfAbsent(getInitialSpanInfoIdentifier(component, suffix, overriddenName),
                                                  identifier -> new ExecutionInitialSpanInfo(component, apiId, overriddenName,
                                                                                             suffix,
                                                                                             tracingLevelConfiguration));

    tracingLevelConfiguration.onConfigurationChange(executionInitialSpanInfo::reconfigureInitialSpanInfo);

    return executionInitialSpanInfo;
  }

  public void initialiseAttributes() {
    if (muleContext.getArtifactType().equals(POLICY)) {
      apiId = configurationProperties.resolveStringProperty(API_ID_CONFIGURATION_PROPERTIES_KEY).orElse(null);
    }
  }

  private InitialSpanInfoIdentifier getInitialSpanInfoIdentifier(Component location, String suffix, String overriddenName) {
    return new InitialSpanInfoIdentifier(location, suffix, overriddenName);
  }
}
