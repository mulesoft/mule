/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.Optional;

/**
 * A factory for {@link SourceAdapter} instances
 */
@FunctionalInterface
public interface SourceAdapterFactory {

  /**
   * Creates a new {@link SourceAdapter}
   *
   * @param configurationInstance an {@link Optional} {@link ConfigurationInstance} in case the source requires a config
   * @param sourceCallbackFactory a {@link SourceCallbackFactory}
   * @return a new {@link SourceAdapter}
   */
  SourceAdapter createAdapter(Optional<ConfigurationInstance> configurationInstance, SourceCallbackFactory sourceCallbackFactory);
}
