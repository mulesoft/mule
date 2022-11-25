/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

/**
 * A {@link ComponentCoreStartSpanInfoProvider} that provides {@link StartSpanInfo}
 * for adding a suffix to the name.
 *
 * @since 4.5.0
 */
public class SuffixComponentStartStartSpanInfoProvider extends ComponentCoreStartSpanInfoProvider {

  private final String routeName;

  public SuffixComponentStartStartSpanInfoProvider(Component component, String routeName) {
    super(component);
    this.routeName = routeName;
  }

  @Override
  public StartSpanInfo get(CoreEvent coreEvent) {
    return new ComponentStartSpanInfo(component, coreEvent, ":" + routeName);
  }
}
