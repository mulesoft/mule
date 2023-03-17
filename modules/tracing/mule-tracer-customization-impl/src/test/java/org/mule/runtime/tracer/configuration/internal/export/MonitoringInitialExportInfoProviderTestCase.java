/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.export;

import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.ASYNC_INNER_CHAIN;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_CACHE_CHAIN;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.UNKNOWN;
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

import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CUSTOMIZATION)
public class MonitoringInitialExportInfoProviderTestCase {

  @Test
  public void testForSpecialCasesByName() {
    testComponent(MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY, TRUE, singleton("execute-next"));
    testComponent(MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY, FALSE, emptySet());
    testComponent(UNKNOWN, FALSE, emptySet());
    testComponent(ASYNC_INNER_CHAIN, FALSE, emptySet());
    testComponent(MULE_CACHE_CHAIN, FALSE, emptySet());
  }

  @Test
  public void testForSpecialCasesByClass() {
    MonitoringInitialExportInfoProvider monitoringInitialExportInfoProvider = new MonitoringInitialExportInfoProvider();
    PolicyChain component = new PolicyChain();
    InitialExportInfo initialExportInfo = monitoringInitialExportInfoProvider.getInitialExportInfo(component);
    assertThat(initialExportInfo.noExportUntil(), equalTo(singleton("execute-next")));
    assertThat(initialExportInfo.isExportable(), equalTo(TRUE));
  }

  private static void testComponent(String componentName, boolean expectedExportable, Set<String> noExportUntil) {
    MonitoringInitialExportInfoProvider monitoringInitialExportInfoProvider = new MonitoringInitialExportInfoProvider();
    InitialExportInfo initialExportInfo = monitoringInitialExportInfoProvider.getInitialExportInfo(componentName);
    assertThat(initialExportInfo.noExportUntil(), equalTo(noExportUntil));
    assertThat(initialExportInfo.isExportable(), equalTo(expectedExportable));
  }
}
