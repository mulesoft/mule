/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

/**
 * A provider for {@link StartSpanInfo} based on {@link CoreEvent}
 *
 * @since 4.5.0
 */
public interface EventBasedStartSpanCustomizationInfoProvider {

  /**
   * @param core a {@link CoreEvent}
   *
   * @return the {@link StartSpanInfo}
   */
  StartSpanInfo get(CoreEvent core);

}
