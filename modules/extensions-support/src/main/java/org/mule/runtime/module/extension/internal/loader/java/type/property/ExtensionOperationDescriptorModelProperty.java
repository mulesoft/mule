/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterWrapper;

import java.util.Optional;

import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isNonBlocking;

/**
 * Binds an {@link OperationModel} with a {@link MethodElement}
 *
 * @since 4.1
 */
public class ExtensionOperationDescriptorModelProperty implements ModelProperty {

  private MethodElement operationMethod;

  public ExtensionOperationDescriptorModelProperty(MethodElement operationMethod) {
    this.operationMethod = operationMethod;
  }

  public MethodElement<? extends Type> getOperationMethod() {
    return operationMethod;
  }

  @Override
  public String getName() {
    return "operation-method-element";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  public Type getOperationReturnType() {
    if (isNonBlocking(getOperationMethod())) {
      Type returnType = ((ParameterWrapper) (getOperationMethod().getParameters().stream()
          .filter(p -> p.getType().isAssignableTo(CompletionCallback.class)).findFirst().get())).getType().getGenerics()
              .get(0).getConcreteType();
      return getPayloadType(returnType);
    } else {
      return getPayloadType(getOperationMethod().getReturnType());
    }
  }

  private Type getPayloadType(Type type) {
    if (type.isAssignableTo(Result.class) && type.getGenerics().size() == 2) {
      return type.getGenerics().get(0).getConcreteType();
    }
    return type;
  }
}
