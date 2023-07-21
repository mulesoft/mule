/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A provider for builders for {@link InitialSpanInfo}
 *
 * @since 4.5.0
 */
public interface InitialSpanInfoProvider {

  /**
   * @param component the {@link Component} to generate the {@link InitialSpanInfo} for.
   *
   * @return a {@link InitialSpanInfo} based on a {@link Component}.
   */
  InitialSpanInfo getInitialSpanInfo(Component component);

  /**
   * @param component the {@link Component} to generate the {@link InitialSpanInfo} for.
   * @param suffix    the suffix.
   * @return
   */
  InitialSpanInfo getInitialSpanInfo(Component component, String suffix);


  /**
   * @param component      the {@link Component} to generate the {@link InitialSpanInfo} for.
   * @param overriddenName the overridden name.
   * @param suffix         the suffix.
   * @return
   */
  InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix);
}
