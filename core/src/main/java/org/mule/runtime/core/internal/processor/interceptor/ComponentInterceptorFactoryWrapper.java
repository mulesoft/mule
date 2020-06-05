/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.function.Supplier;

interface ComponentInterceptorFactoryWrapper extends Supplier<ComponentInterceptorWrapper> {

  boolean isInterceptable(ReactiveProcessor component);

  boolean intercept(ComponentLocation componentLocation);

}
