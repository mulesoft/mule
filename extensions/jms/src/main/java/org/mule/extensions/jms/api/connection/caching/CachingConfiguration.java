/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import org.mule.extensions.jms.internal.connection.JmsCachingConnectionFactory;

/**
 * Provides the configuration elements required to configure a {@link JmsCachingConnectionFactory}
 *
 * @since 4.0
 */
public interface CachingConfiguration {

  int getSessionCacheSize();

  boolean isProducersCache();

  boolean isConsumersCache();
}
