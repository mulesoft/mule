/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import static org.mule.runtime.tracer.customization.api.InternalSpanNames.ASYNC_INNER_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.CACHE_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_SOURCE_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_NEXT_ACTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.UNKNOWN;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CUSTOMIZATION;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.impl.provider.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;

import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CUSTOMIZATION)
public class AbstractInitialExportInfoProviderTestCase {

  @Test
  public void testForSpecialCasesByName() {
    testComponent(POLICY_SOURCE_SPAN_NAME, TRUE, singleton("execute-next"));
    testComponent(POLICY_NEXT_ACTION_SPAN_NAME, FALSE, emptySet());
    testComponent(UNKNOWN, FALSE, emptySet());
    testComponent(ASYNC_INNER_CHAIN_SPAN_NAME, FALSE, emptySet());
    testComponent(CACHE_CHAIN_SPAN_NAME, FALSE, emptySet());
  }

  @Test
  public void testForSpecialCasesByClass() {
    InitialExportInfoProvider monitoringInitialExportInfoProvider = new MonitoringInitialExportInfoProvider();
    PolicyChain component = new PolicyChain();
    InitialExportInfo initialExportInfo = monitoringInitialExportInfoProvider.getInitialExportInfo(component);
    assertThat(initialExportInfo.noExportUntil(), equalTo(singleton("execute-next")));
    assertThat(initialExportInfo.isExportable(), equalTo(TRUE));
  }

  private static void testComponent(String componentName, boolean expectedExportable, Set<String> noExportUntil) {
    InitialExportInfoProvider monitoringInitialExportInfoProvider = new MonitoringInitialExportInfoProvider();
    InitialExportInfo initialExportInfo = monitoringInitialExportInfoProvider.getInitialExportInfo(componentName);
    assertThat(initialExportInfo.noExportUntil(), equalTo(noExportUntil));
    assertThat(initialExportInfo.isExportable(), equalTo(expectedExportable));
  }
}
