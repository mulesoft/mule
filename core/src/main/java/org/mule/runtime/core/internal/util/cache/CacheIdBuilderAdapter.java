/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.cache;

import java.util.List;

/**
 * Helper interface to unify both {@link org.mule.runtime.core.internal.value.cache.ValueProviderCacheId} and
 * {@link org.mule.runtime.core.internal.metadata.cache.MetadataCacheId} construction.
 *
 * @param <K> the cache id this builder creates.
 */
public interface CacheIdBuilderAdapter<K> {

  CacheIdBuilderAdapter<K> withSourceElementName(String name);

  CacheIdBuilderAdapter<K> withHashValue(int value);

  CacheIdBuilderAdapter<K> containing(List<K> parts);

  K build();

}
