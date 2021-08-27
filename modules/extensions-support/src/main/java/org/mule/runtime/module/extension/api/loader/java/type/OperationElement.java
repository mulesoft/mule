/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.List;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isNonBlocking;

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
          .filter(p -> p.getType().isAssignableTo(CompletionCallback.class)
              || p.getType().isAssignableTo(org.mule.sdk.api.runtime.process.CompletionCallback.class))
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

}
