/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.builder;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;

import java.util.Set;

import org.junit.Test;

/**
 * An abstract test for {@link InitialSpanInfoBuilderTestCase}.
 *
 * @since 4.6.0
 */
public abstract class InitialSpanInfoBuilderTestCase {

  public static final String TEST_COMPONENT_NAME = "test";
  public static final String TEST_COMPONENT_NAMESPACE = "test-namespace";
  public static final String TEST_SUFFIX = ":suffix";
  public static final String TEST_COMPONENT_UNTIL_NAME = "component";

  @Test
  public void testExportableInitialSpanInfo() {
    InitialSpanInfoBuilder componentInitialSpanInfoBuilder = getInitialSpanInfoBuilder();
    InitialSpanInfo initialSpanInfo = componentInitialSpanInfoBuilder.build();
    assertInitialSpanInfo(initialSpanInfo, TEST_COMPONENT_NAMESPACE + ":" + TEST_COMPONENT_NAME, true, emptySet());
  }

  @Test
  public void testExportableInitialSpanInfoWithSuffix() {
    InitialSpanInfoBuilder componentInitialSpanInfoBuilder = getInitialSpanInfoBuilder();
    InitialSpanInfo initialSpanInfo = componentInitialSpanInfoBuilder.withSuffix(TEST_SUFFIX).build();
    assertInitialSpanInfo(initialSpanInfo, TEST_COMPONENT_NAMESPACE + ":" + TEST_COMPONENT_NAME + TEST_SUFFIX, true, emptySet());
  }

  @Test
  public void testExportableInitialSpanInfoNoExport() {
    InitialSpanInfoBuilder componentInitialSpanInfoBuilder = getInitialSpanInfoBuilder();
    InitialSpanInfo initialSpanInfo = componentInitialSpanInfoBuilder.withNoExport().build();
    assertInitialSpanInfo(initialSpanInfo, TEST_COMPONENT_NAMESPACE + ":" + TEST_COMPONENT_NAME, false, emptySet());
  }

  @Test
  public void testExportableInitialSpanInfoWithForceNotExportUntil() {
    InitialSpanInfoBuilder componentInitialSpanInfoBuilder = getInitialSpanInfoBuilder();
    InitialSpanInfo initialSpanInfo = componentInitialSpanInfoBuilder.withForceNoExportUntil(TEST_COMPONENT_UNTIL_NAME).build();
    assertInitialSpanInfo(initialSpanInfo, TEST_COMPONENT_NAMESPACE + ":" + TEST_COMPONENT_NAME, true,
                          singleton(TEST_COMPONENT_UNTIL_NAME));
  }

  private static void assertInitialSpanInfo(InitialSpanInfo initialSpanInfo, String spanName, boolean export,
                                            Set<String> noExportUntil) {
    assertThat(initialSpanInfo.getInitialExportInfo().isExportable(), equalTo(export));
    assertThat(initialSpanInfo.getInitialExportInfo().noExportUntil(), equalTo(noExportUntil));
    assertThat(initialSpanInfo.isRootSpan(), equalTo(FALSE));
    assertThat(initialSpanInfo.getName(), equalTo(spanName));
  }

  abstract InitialSpanInfoBuilder getInitialSpanInfoBuilder();
}
