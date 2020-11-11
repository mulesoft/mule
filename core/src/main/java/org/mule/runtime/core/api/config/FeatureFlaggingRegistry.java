/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.function.Predicate;

/**
 * @TODO document
 * @since 4.4.0
 */
public interface FeatureFlaggingRegistry {

  void registerFeature(String feature, Predicate<MuleContext> condition);

  Map<String, Predicate<MuleContext>> getFeatureConfigurations();
}
