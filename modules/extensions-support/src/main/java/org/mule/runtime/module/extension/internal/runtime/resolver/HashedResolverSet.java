/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.MuleContext;

/**
 * {@link ResolverSet} implementation which uses {@link HashedResolverSetResult} to storage the resolved values.
 * This gives the capability of comparing {@link ResolverSetResult} a quickly verify if two {@link ResolverSetResult}
 * are equals or not.
 *
 * @since 4.0
 */
public class HashedResolverSet extends ResolverSet {

  public HashedResolverSet(MuleContext muleContext) {
    super(muleContext);
  }

  @Override
  ResolverSetResult.Builder getResolverSetBuilder() {
    return HashedResolverSetResult.newBuilder();
  }
}
