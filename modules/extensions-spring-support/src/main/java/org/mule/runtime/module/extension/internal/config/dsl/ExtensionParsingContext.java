/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.singleton;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context to be used while registering parsers for an {@link ExtensionModel} definition, to keep track of global data accross all
 * the different definition parsers
 *
 * @since 4.0
 */
public class ExtensionParsingContext {

  private final Map<String, MetadataType> parsedObjectTypes = new HashMap<>();
  private final TypeCatalog typeCatalog;

  public ExtensionParsingContext(ExtensionModel extensionModel) {
    typeCatalog = TypeCatalog.getDefault(singleton(extensionModel));
  }

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
   * Returns a {@link Collection} with all the declared {@link MetadataType} subtypes for the indicated {@link MetadataType}
   * {@code type}.
   * <p>
   * Lookup will be performed first by {@link TypeIdAnnotation typeId}, defaulting to {@link MetadataType type} comparison if no
   * {@link TypeIdAnnotation typeId} was found
   *
   * @param type the {@link MetadataType} for which to retrieve its declared subTypes
   * @return a {@link Collection} with all the declared subtypes for the indicated {@link MetadataType}
   */
  public Collection<ObjectType> getSubTypes(ObjectType type) {
    return typeCatalog.getSubTypes(type);
  }

  /**
   * @return a {@link List} with all the types that are extended by another type
   */
  public List<MetadataType> getAllBaseTypes() {
    return copyOf(typeCatalog.getAllBaseTypes());
  }

  /**
   * @return a {@link List} with all the types which extend another type, in no particular order
   */
  public List<MetadataType> getAllSubTypes() {
    return copyOf(typeCatalog.getAllSubTypes());
  }

  private String generateObjectKey(String name, String namespace) {
    return name + namespace;
  }

}
