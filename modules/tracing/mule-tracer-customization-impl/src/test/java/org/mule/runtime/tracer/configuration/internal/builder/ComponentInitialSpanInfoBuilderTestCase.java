/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.builder;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class ComponentInitialSpanInfoBuilderTestCase extends InitialSpanInfoBuilderTestCase {

  @Override
  InitialSpanInfoBuilder getInitialSpanInfoBuilder() {
    Component mockedComponent = getMockedComponent();
    ComponentInitialSpanInfoBuilder componentInitialSpanInfoBuilder = new ComponentInitialSpanInfoBuilder(mockedComponent);
    return componentInitialSpanInfoBuilder;
  }

  private static Component getMockedComponent() {
    Component mockedComponent = mock(Component.class);
    ComponentIdentifier mockedComponentIdentifier = mock(ComponentIdentifier.class);
    when(mockedComponentIdentifier.getName()).thenReturn(TEST_COMPONENT_NAME);
    when(mockedComponentIdentifier.getNamespace()).thenReturn(TEST_COMPONENT_NAMESPACE);
    when(mockedComponent.getIdentifier()).thenReturn(mockedComponentIdentifier);
    return mockedComponent;
  }
}
