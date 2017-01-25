/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.model.resolver;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.impl.BaseMetadataType;
import org.mule.metadata.json.JsonTypeLoader;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single type (commonly used in JSON schemas).
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public class SingleTypeResolver implements TypeResolver {

  private String typeIdentifier;
  private final TypeLoader typeLoader;

  public SingleTypeResolver(String typeIdentifier, URL schemaUrl) {
    Preconditions.checkNotNull(typeIdentifier);
    Preconditions.checkNotNull(schemaUrl);
    typeLoader = new JsonTypeLoader(FileUtils.toFile(schemaUrl));
    this.typeIdentifier = typeIdentifier;
  }

  @Override
  public Optional<MetadataType> resolveType(String typeIdentifier) {
    return this.typeIdentifier.equals(typeIdentifier) ? getTypeWhileAddingIDToMakeItSerializable(typeIdentifier)
        : Optional.empty();
  }

  /**
   * TODO(fernandezlautaro): MULE-11508 this method is needed for Mozart consumption of the serialized ExtensionModel, we need to force an ID on the type or it fails when doing the ExtensionModelJsonSerializer#serialize
   * @param typeIdentifier
   * @return
   */
  private Optional<MetadataType> getTypeWhileAddingIDToMakeItSerializable(String typeIdentifier) {
    final Optional<MetadataType> load = typeLoader.load(typeIdentifier);
    load.ifPresent(metadataType -> {
      if (metadataType instanceof ObjectType) {
        try {
          final Field annotationsField = BaseMetadataType.class.getDeclaredField("annotations");
          annotationsField.setAccessible(true);
          Map<Class<? extends TypeAnnotation>, TypeAnnotation> mapa =
              (Map<Class<? extends TypeAnnotation>, TypeAnnotation>) annotationsField.get(metadataType);
          mapa.put(TypeIdAnnotation.class, new TypeIdAnnotation(typeIdentifier));
        } catch (NoSuchFieldException | IllegalAccessException e) {
          e.printStackTrace();
          throw new RuntimeException("this code must be removed", e);
        }
      }
    });
    return load;
  }
}
