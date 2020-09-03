/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.sdk.api.runtime.operation.Result;

import java.util.Collection;

public class TransformingLegacyResultAdapterCollection extends TransformingCollection<Result> {

  public TransformingLegacyResultAdapterCollection(Collection<Object> delegate) {
    super(delegate, Result.class, SdkResultAdapter::from);
  }
}
