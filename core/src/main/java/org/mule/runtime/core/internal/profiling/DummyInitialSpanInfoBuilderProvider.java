package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilderProvider;

/**
 * A dummy implementation of {@link InitialSpanInfoBuilderProvider}.
 *
 * @since 4.6.0
 */
public class DummyInitialSpanInfoBuilderProvider implements InitialSpanInfoBuilderProvider {

  private static final DummyInitialSpanInfoBuilderProvider INSTANCE = new DummyInitialSpanInfoBuilderProvider();

  public static InitialSpanInfoBuilderProvider getDummyInitialSpanInfoBuilderProvider() {
    return INSTANCE;
  }

  @Override
  public InitialSpanInfoBuilder getComponentInitialSpanInfoBuilder(Component component) {
    return DummyComponentInitialSpanInfoBuilder.getDummyInitialSpanInfoBuilder();
  }

  @Override
  public InitialSpanInfoBuilder getGenericInitialSpanInfoBuilder() {
    return DummyComponentInitialSpanInfoBuilder.getDummyInitialSpanInfoBuilder();
  }

  private static class DummyComponentInitialSpanInfoBuilder implements InitialSpanInfoBuilder {

    private static final DummyComponentInitialSpanInfoBuilder INSTANCE = new DummyComponentInitialSpanInfoBuilder();

    public static InitialSpanInfoBuilder getDummyInitialSpanInfoBuilder() {
      return INSTANCE;
    }

    @Override
    public InitialSpanInfoBuilder withName(String name) {
      return this;
    }

    @Override
    public InitialSpanInfoBuilder withSuffix(String suffix) {
      return this;
    }

    @Override
    public InitialSpanInfoBuilder withForceNotExportUntil(String componentName) {
      return this;
    }

    @Override
    public InitialSpanInfo build() {
      return DummyInitialSpanInfo.getDummyInitialSpanInfo();
    }

    @Override
    public InitialSpanInfoBuilder withNoExport() {
      return this;
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
}
