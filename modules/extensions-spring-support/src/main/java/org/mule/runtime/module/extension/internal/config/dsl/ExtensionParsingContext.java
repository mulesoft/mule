/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Context to be used while registering parsers for an {@link ExtensionModel} definition, to keep track of global data accross all
 * the different definition parsers
 *
 * @since 4.0
 */
public class ExtensionParsingContext {

  private final Map<String, MetadataType> parsedObjectTypes = new HashMap<>();
  private Optional<SubTypesMappingContainer> subTypesMapping = empty();

  /**
   * Register an {@link ObjectType} to indicate it has already being parsed for the given {@code name} and {@code namespace}
   *
   * @param name the {@code name} associated to the parsed {@code type}
   * @param namespace the {@code namespace} associated to the parsed {@code type}
   * @param type the parsed {@link ObjectType type}
   * @return {@code true} if the object was registered, {@code false} if a previous definition existed
   */
  public boolean registerObjectType(String name, String namespace, ObjectType type) {
    return parsedObjectTypes.put(generateObjectKey(name, namespace), type) == null;
  }

  /**
   * @param name the {@code name} of the element
   * @param namespace the {@code namespace} of the element
   * @return {@code true} if an {@link ObjectType} with the given {@code name} and {@code namespace} was registered in the current
   *         context
   */
  public boolean isRegistered(String name, String namespace) {
    return parsedObjectTypes.containsKey(generateObjectKey(name, namespace));
  }

  /**
   * Returns a {@link List} with all the declared {@link MetadataType} subtypes for the indicated {@link MetadataType}
   * {@code type}.
   * <p>
   * Lookup will be performed first by {@link TypeIdAnnotation typeId}, defaulting to {@link MetadataType type} comparison if no
   * {@link TypeIdAnnotation typeId} was found
   *
   * @param type the {@link MetadataType} for which to retrieve its declared subTypes
   * @return a {@link List} with all the declared subtypes for the indicated {@link MetadataType}
   */
  public List<MetadataType> getSubTypes(MetadataType type) {
    return subTypesMapping.map(mapping -> mapping.getSubTypes(type)).orElse(ImmutableList.of());
  }

  /**
   * Configures the subtype mapping to use
   *
   * @param subTypesMapping a {@link SubTypesMappingContainer}
   */
  public void setSubTypesMapping(SubTypesMappingContainer subTypesMapping) {
    this.subTypesMapping = ofNullable(subTypesMapping);
  }

  /**
   * @return a {@link List} with all the types that are extended by another type
   */
  public List<MetadataType> getAllBaseTypes() {
    return subTypesMapping.map(SubTypesMappingContainer::getAllBaseTypes).orElse(ImmutableList.of());
  }

  /**
   * @return a {@link List} with all the types which extend another type, in no particular order
   */
  public List<MetadataType> getAllSubTypes() {
    return subTypesMapping.map(SubTypesMappingContainer::getAllSubTypes).orElse(ImmutableList.of());
  }

  private String generateObjectKey(String name, String namespace) {
    return name + namespace;
  }

}
