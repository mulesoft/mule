/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoProvider;

/**
 * A dummy implementation of {@link InitialSpanInfoProvider}.
 *
 * @since 4.6.0
 */
public class DummyInitialSpanInfoProvider implements InitialSpanInfoProvider {

  private static final DummyInitialSpanInfoProvider INSTANCE = new DummyInitialSpanInfoProvider();

  private static final DummyInitialSpanInfo DUMMY_INITIAL_SPAN_INFO_INSTANCE = new DummyInitialSpanInfo();

  public static DummyInitialSpanInfoProvider getDummyInitialSpanInfoProvider() {
    return INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component, String suffix) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(String name) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component, String overriddenName, String suffix) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  private static class DummyInitialSpanInfo implements InitialSpanInfo {

    public static final String DUMMY_SPAN = "dummy-span";
    private static final DummyInitialSpanInfo INSTANCE = new DummyInitialSpanInfo();

    public static InitialSpanInfo getDummyInitialSpanInfo() {
      return INSTANCE;
    }

    @Override
    public String getName() {
      return DUMMY_SPAN;
    }

    @Override
    public InitialExportInfo getInitialExportInfo() {
      return NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
    }
  }
}
