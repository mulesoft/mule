/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A provider for builders for {@link InitialSpanInfo}
 *
 * @since 4.6.0
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
   * @param name the name for the span.
   *
   * @return a {@link InitialSpanInfo} based on a name.
   */
  InitialSpanInfo getInitialSpanInfo(String name);


  /**
   * @param name the name for the span.
   *
   * @return a {@link InitialSpanInfo} based on a name taking into account that this is a debug level span.
   */
  InitialSpanInfo getDebugLevelInitialSpanInfo(String name);


  /**
   * @param component      the {@link Component} to generate the {@link InitialSpanInfo} for.
   * @param overriddenName the overridden name.
   * @param suffix         the suffix.
   * @return
   */
  InitialSpanInfo getInitialSpanInfo(Component component, String overriddenName, String suffix);
}
