/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.values;

import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.values.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;

import java.util.List;
import java.util.Map;

/**
 * Utility class for {@link ValuesProviderMediator}
 *
 * @since 4.0
 */
class ValuesProviderMediatorUtils {

  /**
   * Given a {@link Value}, this is navigated recursively cloning each {@link Value} of the tree structure creating a
   * {@link ValueBuilder} and adding the partName of each {@link Value} found.
   *
   * @param value            {@link Value} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link Value}
   * @return a {@link ValueBuilder} with the cloned and enriched values
   */
  static ValueBuilder cloneAndEnrichValue(Value value, Map<Integer, String> partOrderMapping) {
    return cloneAndEnrichValue(value, partOrderMapping, 1);
  }

  static ValueBuilder cloneAndEnrichValue(Value value, List<ParameterModel> parameters) {
    return cloneAndEnrichValue(value, orderParts(parameters), 1);
  }

  /**
   * Given a {@link Value}, this is navigated recursively cloning each {@link Value} of the tree structure creating a
   * {@link ValueBuilder} and adding the partName of each {@link Value} found.
   *
   * @param value            {@link Value} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link Value}
   * @param level            the current level of the part of the {@link Value} to be cloned and enriched
   * @return a {@link ValueBuilder} with the cloned and enriched values
   */
  static ValueBuilder cloneAndEnrichValue(Value value, Map<Integer, String> partOrderMapping, int level) {
    final ValueBuilder keyBuilder =
        ValueBuilder.newValue(value.getId(), partOrderMapping.get(level)).withDisplayName(value.getDisplayName());
    value.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichValue(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }

  private static Map<Integer, String> orderParts(List<ParameterModel> parameters) {
    return parameters.stream()
        .collect(toMap(param -> param.getValueProviderModel().get().getPartOrder(), NamedObject::getName));
  }

}
