/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;

/**
 * A dummy implementation of {@link InitialSpanInfoProvider}.
 *
 * @since 4.5.0
 */
public class DummyInitialSpanInfoProvider implements InitialSpanInfoProvider {

  private static final DummyInitialSpanInfoProvider INSTANCE = new DummyInitialSpanInfoProvider();

  private static final DummyInitialSpanInfo DUMMY_INITIAL_SPAN_INFO_INSTANCE = new DummyInitialSpanInfo();

  public static DummyInitialSpanInfoProvider getDummyInitialSpanInfoProvider() {
    return INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String suffix) {
    return DUMMY_INITIAL_SPAN_INFO_INSTANCE;
  }

  @Override
  public InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix) {
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
