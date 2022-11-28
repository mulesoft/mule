/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling.tracing;

import org.mule.runtime.core.api.tracing.customization.EventBasedInitialSpanInfoProvider;

/**
 * Interface which allows classes to set {@link EventBasedInitialSpanInfoProvider}
 *
 * @since 4.5.0
 */
public interface EventBasedInitialSpanInfoProviderAware {

  void setEventBasedInitialSpanInfoProvider(
                                            EventBasedInitialSpanInfoProvider eventBasedStartStartInfoProvider);
}
