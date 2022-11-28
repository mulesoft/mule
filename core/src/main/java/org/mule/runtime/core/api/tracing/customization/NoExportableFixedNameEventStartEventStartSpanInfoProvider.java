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
 * A {@link FixedNameCoreEventStartEventStartSpanInfoProvider} to provide {@link NoExportableFixedNameStartSpanInfo}
 *
 * @since 4.5.0
 */
public class NoExportableFixedNameEventStartEventStartSpanInfoProvider
    extends FixedNameCoreEventStartEventStartSpanInfoProvider {

  public NoExportableFixedNameEventStartEventStartSpanInfoProvider(String name) {
    super(name);
  }

  @Override
  public StartSpanInfo get(CoreEvent coreEvent) {
    return new NoExportableFixedNameStartSpanInfo(name);
  }
}
