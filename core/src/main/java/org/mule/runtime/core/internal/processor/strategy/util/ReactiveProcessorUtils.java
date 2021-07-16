/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.util;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;
import org.mule.runtime.core.privileged.processor.chain.HasLocation;

/**
 * Utility methods for handling {@ReactiveProcessors}'s.
 * <p>
 * MULE-19594: Handle expression problem for Reactive Processor (ComponentLocation)
 *
 * @since 4.0
 */
public final class ReactiveProcessorUtils {

  /**
   * @param processor the reactive processor from which the {@link ComponentLocation} has to be extracted.
   * @return the {@link ComponentLocation} if existing.
   */
  public static ComponentLocation getLocation(ReactiveProcessor processor) {
    if (processor instanceof HasLocation) {
      return ((HasLocation) processor).resolveLocation();
    }

    if (processor instanceof InterceptedReactiveProcessor) {
      return getLocation(((InterceptedReactiveProcessor) processor).getProcessor());
    }

    if (processor instanceof Component) {
      return ((Component) processor).getLocation();
    }

    if (processor instanceof ComponentInnerProcessor) {
      return ((ComponentInnerProcessor) processor).getLocation();
    }

    return null;
  }

}
