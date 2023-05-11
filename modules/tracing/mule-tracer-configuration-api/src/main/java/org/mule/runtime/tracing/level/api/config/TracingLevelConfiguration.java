/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.api.config;

import java.util.Map;

/**
 * Allows to configure the desired tracing level
 *
 * @since 4.6.0
 */
public interface TracingLevelConfiguration {

  TracingLevel getTracingLevel();

  Map<String, TracingLevel> getTracingLevelOverrides();
}
