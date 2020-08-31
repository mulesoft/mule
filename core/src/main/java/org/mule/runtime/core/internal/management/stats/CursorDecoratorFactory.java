/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Provides a {@link CursorComponentDecoratorFactory} for a given component according to its {@link Component#getLocation()
 * location}.
 *
 * @since 4.4, 4.3.1
 */
public interface CursorDecoratorFactory {

  /**
   * Creates a factory that will decorate cursors, to populate the {@link PayloadStatistics} for a specific component.
   *
   * @param component the component that will interact with the decorated {@link Iterator} or {@link InputStream}.
   * @return the decorator factory for the provided component.
   */
  CursorComponentDecoratorFactory componentDecoratorFactory(Component component);

}
