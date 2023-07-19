/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.List;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.isNonBlocking;

/**
 * {@link MethodElement} specification for Operations
 *
 * @since 4.1
 */
@NoImplement
public interface OperationElement extends MethodElement<OperationContainerElement> {

  default MetadataType getOperationReturnMetadataType() {
    Type returnType;
    if (isNonBlocking(this)) {
      returnType = getParameters().stream()
          .filter(JavaExtensionModelParserUtils::isCompletionCallbackParameter)
          .findAny()
          .get()
          .getType();
      List<TypeGeneric> generics = returnType.getGenerics();
      if (generics.isEmpty()) {
        return BaseTypeBuilder.create(JAVA).anyType().build();
      }
      returnType = generics.get(0).getConcreteType();
    } else {
      returnType = getReturnType();
    }
    return IntrospectionUtils.getReturnType(returnType);
  }

  default MetadataType getOperationAttributesMetadataType() {
    Type returnType;
    if (isNonBlocking(this)) {
      returnType = getParameters().stream()
          .filter(JavaExtensionModelParserUtils::isCompletionCallbackParameter)
          .findAny()
          .get()
          .getType();
      List<TypeGeneric> generics = returnType.getGenerics();
      if (generics.isEmpty()) {
        return BaseTypeBuilder.create(JAVA).anyType().build();
      }
      return generics.get(1).getConcreteType().asMetadataType();
    } else {
      return getAttributesMetadataType();
    }
  }
}
