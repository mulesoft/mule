/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.extension.api.introspection.ExtensionModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Context to be used while registering parsers for an {@link ExtensionModel} definition, to keep track of global data accross all
 * the different definition parsers
 *
 * @since 4.0
 */
public class ExtensionParsingContext {

  private final Map<String, MetadataType> parsedObjectTypes = new HashMap<>();

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

  private String generateObjectKey(String name, String namespace) {
    return name + namespace;
  }
}
