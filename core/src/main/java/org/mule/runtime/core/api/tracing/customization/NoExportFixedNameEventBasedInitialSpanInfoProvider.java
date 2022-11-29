/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A {@link FixedNameEventBasedInitialSpanInfoProvider} to provide {@link NoExportFixedNameInitialSpanInfo}
 *
 * @since 4.5.0
 */
public class NoExportFixedNameEventBasedInitialSpanInfoProvider
    extends FixedNameEventBasedInitialSpanInfoProvider {

  public NoExportFixedNameEventBasedInitialSpanInfoProvider(String name) {
    super(name);
  }

  @Override
  public InitialSpanInfo get(CoreEvent coreEvent) {
    return new NoExportFixedNameInitialSpanInfo(name);
  }
}
