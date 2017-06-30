/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.values;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.values.Value;
import org.mule.runtime.api.values.ValueBuilder;
import org.mule.runtime.module.extension.internal.metadata.MultilevelMetadataKeyBuilder;

import java.util.Map;

class ValuesProviderMediatorUtils {

  static ValueBuilder cloneAndEnrichMetadataKey(Value key, Map<Integer, String> partOrderMapping) {
    return cloneAndEnrichMetadataKey(key, partOrderMapping, 1);
  }

  /**
   * Given a {@link MetadataKey}, this is navigated recursively cloning each {@link MetadataKey} of the tree structure creating a
   * {@link MultilevelMetadataKeyBuilder} and adding the partName of each {@link MetadataKey} found.
   *
   * @param key              {@link MetadataKey} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link MetadataKey}
   * @param level            the current level of the part of the {@link MetadataKey} to be cloned and enriched
   * @return a {@link MetadataKeyBuilder} with the cloned and enriched keys
   */
  static ValueBuilder cloneAndEnrichMetadataKey(Value key, Map<Integer, String> partOrderMapping, int level) {
    final ValueBuilder keyBuilder =
        ValueBuilder.newValue(key.getId(), partOrderMapping.get(level)).withDisplayName(key.getDisplayName());
    key.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichMetadataKey(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }

}
