/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CUSTOMIZATION;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.chain.UnnamedComponent;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CUSTOMIZATION)
public class DefaultInitialSpanInfoProviderTestCase {

  public static final String SUFFIX = "suffix";
  public static final String OVERRIDDEN_NAME = "overriddenName";
  public static final String LOCATION = "location";
  public static final String NAMESPACE = "namespace";
  public static final String COMPONENT_NAME = "component";

  @Test
  public void testCache() {
    DefaultInitialSpanInfoProvider defaultInitialSpanInfoProvider = new DefaultInitialSpanInfoProvider();
    defaultInitialSpanInfoProvider.setConfigurationProperties(mock(ConfigurationProperties.class));
    TracingLevelConfiguration mockedTracingLevelConfiguration = mock(TracingLevelConfiguration.class);
    when(mockedTracingLevelConfiguration.getTracingLevel()).thenReturn(MONITORING);
    when(mockedTracingLevelConfiguration.getTracingLevelOverride(any())).thenReturn(MONITORING);
    defaultInitialSpanInfoProvider.seTracingLevelConfiguration(mockedTracingLevelConfiguration);
    MuleContext mockedMuleContext = mock(MuleContext.class);
    when(mockedMuleContext.getArtifactType()).thenReturn(APP);
    defaultInitialSpanInfoProvider.setMuleContext(mockedMuleContext);
    Component component = mock(Component.class);
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    ComponentIdentifier mockedComponentIdentifier = mock(ComponentIdentifier.class);
    when(mockedComponentIdentifier.getNamespace()).thenReturn(NAMESPACE);
    when(mockedComponentIdentifier.getName()).thenReturn(COMPONENT_NAME);
    when(component.getIdentifier()).thenReturn(mockedComponentIdentifier);
    when(componentLocation.getLocation()).thenReturn(LOCATION);
    when(component.getLocation()).thenReturn(componentLocation);
    InitialSpanInfo initialSpanInfo = defaultInitialSpanInfoProvider.getInitialSpanInfo(component);
    InitialSpanInfo initialSpanInfoWithSuffix = defaultInitialSpanInfoProvider.getInitialSpanInfo(component, SUFFIX);
    InitialSpanInfo initialSpanInfoWithOverriddenName =
        defaultInitialSpanInfoProvider.getInitialSpanInfo(component, OVERRIDDEN_NAME, SUFFIX);
    InitialSpanInfo initialSpanInfoWithoutLocation =
        defaultInitialSpanInfoProvider.getInitialSpanInfo(UnnamedComponent.getUnnamedComponent(), OVERRIDDEN_NAME, SUFFIX);

    assertThat(initialSpanInfo.getName(), equalTo(NAMESPACE + ":" + COMPONENT_NAME));
    assertThat(initialSpanInfoWithSuffix.getName(), equalTo(NAMESPACE + ":" + COMPONENT_NAME + SUFFIX));
    assertThat(initialSpanInfoWithOverriddenName.getName(), equalTo(OVERRIDDEN_NAME));
    assertThat(initialSpanInfoWithoutLocation.getName(), equalTo(OVERRIDDEN_NAME));

    assertThat(defaultInitialSpanInfoProvider.isCached(((LazyInitialSpanInfo) initialSpanInfo).getDelegate()), equalTo(true));
    assertThat(defaultInitialSpanInfoProvider.isCached(((LazyInitialSpanInfo) initialSpanInfoWithSuffix).getDelegate()),
               equalTo(true));
    assertThat(defaultInitialSpanInfoProvider.isCached(((LazyInitialSpanInfo) initialSpanInfoWithSuffix).getDelegate()),
               equalTo(true));
    assertThat(defaultInitialSpanInfoProvider.isCached(((LazyInitialSpanInfo) initialSpanInfoWithoutLocation).getDelegate()),
               equalTo(false));
  }
}
