/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.toSubTypesMap;
import static org.mule.runtime.extension.api.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getAlias;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isInstantiable;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.module.extension.internal.util.MetadataTypeUtils;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ModelValidator} which applies to {@link ExtensionModel}s.
 * <p>
 * This validator checks that all {@link ExtensionModel Extension} global elements declarations like {@link SubTypesModel
 * Subtypes} or {@link ImportedTypeModel Imported types} are valid.
 *
 * @since 4.0
 */
public final class SubtypesModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    final Map<MetadataType, Set<MetadataType>> typesMapping = toSubTypesMap(model.getSubTypes());
    validateBaseTypeNotFinal(model, typesMapping);
    validateNonAbstractSubtypes(model, typesMapping);
    validateSubtypesExtendOrImplementBaseType(model, typesMapping);
    validateSubtypesNameClashing(model, typesMapping);
  }

  private void validateBaseTypeNotFinal(ExtensionModel model, Map<MetadataType, Set<MetadataType>> typesMapping) {
    List<String> finalBaseTypes = typesMapping.keySet().stream()
        .filter(MetadataTypeUtils::isFinal)
        .map(MetadataTypeUtils::getId)
        .collect(toList());

    if (!finalBaseTypes.isEmpty()) {
      throw new IllegalModelDefinitionException(format("All the declared SubtypesMapping in extension %s should have non final base types, but [%s] are final",
                                                       model.getName(), Arrays.toString(finalBaseTypes.toArray())));
    }
  }

  private void validateNonAbstractSubtypes(ExtensionModel model, Map<MetadataType, Set<MetadataType>> typesMapping) {
    List<String> abstractSubtypes = new LinkedList<>();
    for (Set<MetadataType> subtypes : typesMapping.values()) {
      abstractSubtypes.addAll(subtypes.stream().filter(s -> !isInstantiable(s))
          .map(MetadataTypeUtils::getId).collect(toList()));
    }

    if (!abstractSubtypes.isEmpty()) {
      throw new IllegalModelDefinitionException(format(
                                                       "All the declared Subtypes in extension %s should be of concrete types, but [%s] are non instantiable",
                                                       model.getName(), Arrays.toString(abstractSubtypes.toArray())));
    }
  }

  private void validateSubtypesExtendOrImplementBaseType(ExtensionModel model,
                                                         Map<MetadataType, Set<MetadataType>> typesMapping) {
    for (Map.Entry<MetadataType, Set<MetadataType>> subtypes : typesMapping.entrySet()) {
      if (!subtypes.getKey().getMetadataFormat().equals(JAVA)) {
        continue;
      }

      final Class<?> baseType = getType(subtypes.getKey());
      List<String> invalidTypes = subtypes.getValue().stream().map(JavaTypeUtils::getType)
          .filter(s -> !baseType.isAssignableFrom(s)).map(Class::getSimpleName).collect(toList());

      if (!invalidTypes.isEmpty()) {
        throw new IllegalModelDefinitionException(format("All the declared Subtypes in extension %s should be concrete implementations of the give baseType,"
            + " but [%s] are not implementations of [%s]",
                                                         model.getName(), Arrays.toString(invalidTypes.toArray()),
                                                         baseType.getSimpleName()));
      }
    }
  }

  private void validateSubtypesNameClashing(ExtensionModel model, Map<MetadataType, Set<MetadataType>> typesMapping) {

    ImmutableList<MetadataType> mappedTypes = ImmutableList.<MetadataType>builder()
        .addAll(typesMapping.keySet())
        .addAll(typesMapping.values().stream().flatMap(Collection::stream).collect(toList()))
        .build();

    Map<String, MetadataType> typesByName = new HashMap<>();
    for (MetadataType type : mappedTypes) {
      MetadataType previousType = typesByName.put(getTopLevelTypeName(type), type);
      if (previousType != null && !previousType.equals(type)) {
        throw new IllegalModelDefinitionException(format(
                                                         "Subtypes mapped Type [%s] with alias [%s] in extension [%s] should have a"
                                                             + " different alias name than the previous mapped type [%s]",
                                                         getAlias(type), getTopLevelTypeName(type), model.getName(),
                                                         getAlias(previousType)));
      }
    }
  }

}
