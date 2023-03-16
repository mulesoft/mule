/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.builder;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class GenericInitialSpanInfoBuilderTestCase extends InitialSpanInfoBuilderTestCase {

  @Override
  InitialSpanInfoBuilder getInitialSpanInfoBuilder() {
    return new GenericInitialSpanInfoBuilder().withName(TEST_COMPONENT_NAMESPACE + ":" + TEST_COMPONENT_NAME);
  }
}
