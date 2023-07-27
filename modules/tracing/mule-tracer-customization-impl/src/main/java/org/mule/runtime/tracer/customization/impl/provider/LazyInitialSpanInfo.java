/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A {@link InitialSpanInfo} that is lazily resolved. In case a
 * {@link org.mule.runtime.core.internal.profiling.NoopCoreEventTracer} is used, this will not be invoked.
 *
 * @since 4.5.0
 */
public class LazyInitialSpanInfo implements InitialSpanInfo {

  private final LazyValue<InitialSpanInfo> lazyInitialSpanInfo;

  public LazyInitialSpanInfo(Supplier<InitialSpanInfo> initialSpanInfoSupplier) {
    this.lazyInitialSpanInfo = new LazyValue<>(initialSpanInfoSupplier);
  }

  @Override
  public String getName() {
    return lazyInitialSpanInfo.get().getName();
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return lazyInitialSpanInfo.get().getInitialExportInfo();
  }

  @Override
  public int getInitialAttributesCount() {
    return lazyInitialSpanInfo.get().getInitialAttributesCount();
  }

  @Override
  public Map<String, String> getInitialAttributes() {
    return lazyInitialSpanInfo.get().getInitialAttributes();
  }

  @Override
  public boolean isPolicySpan() {
    return lazyInitialSpanInfo.get().isPolicySpan();
  }

  @Override
  public boolean isRootSpan() {
    return lazyInitialSpanInfo.get().isRootSpan();
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    lazyInitialSpanInfo.get().forEachAttribute(biConsumer);
  }

  public boolean isComputed() {
    return lazyInitialSpanInfo.isComputed();
  }

  public InitialSpanInfo getDelegate() {
    return lazyInitialSpanInfo.get();
  }
}
