/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.core.internal.util.collection.TransformingCollection;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.Collection;

/**
 * Specialization of {@link TransformingCollection} which uses the {@link SdkResultAdapter#from(Object)} method to lazily
 * transform a collection of {@link org.mule.runtime.extension.api.runtime.operation.Result} objects into a collection of
 * {@link Result}
 *
 * @since 4.4.0
 */
public class TransformingLegacyResultAdapterCollection extends TransformingCollection<Result> {

  public TransformingLegacyResultAdapterCollection(Collection<Object> delegate) {
    super(delegate, SdkResultAdapter::from, Result.class);
  }
}
