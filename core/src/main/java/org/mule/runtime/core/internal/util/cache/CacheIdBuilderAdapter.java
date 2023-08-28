/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
