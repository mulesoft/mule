/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.loader.enricher.MetadataTypeEnricher;

import java.io.InputStream;

import org.springframework.core.ResolvableType;

/**
 * Wrapper for {@link Class} of parameter method that provide utility methods to facilitate the introspection of the
 * {@link Class}
 *
 * @since 4.2
 */
public class ParameterTypeWrapper extends TypeWrapper {

  public ParameterTypeWrapper(Class<?> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
  }

  public ParameterTypeWrapper(ResolvableType resolvableType, ClassTypeLoader typeLoader) {
    super(resolvableType, typeLoader);
  }

  @Override
  public MetadataType asMetadataType() {
    MetadataType metadataType = typeLoader.load(type);

    if (this.isSameType(Object.class) ||
        this.isAssignableTo(InputStream.class) ||
        this.isAssignableTo(Byte[].class) ||
        this.isAssignableTo(byte[].class)) {

      MetadataTypeEnricher enricher = new MetadataTypeEnricher();
      return enricher.enrich(BaseTypeBuilder.create(JAVA).anyType().build(), typeLoader.load(type).getAnnotations());
    }

    return metadataType;
  }

}
