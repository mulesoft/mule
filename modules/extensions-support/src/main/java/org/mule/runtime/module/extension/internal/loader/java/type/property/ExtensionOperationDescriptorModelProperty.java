/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

/**
 * Binds an {@link OperationModel} with a {@link MethodElement}
 *
 * @since 4.1
 */
public class ExtensionOperationDescriptorModelProperty implements ModelProperty {

  private OperationElement operationElement;

  public ExtensionOperationDescriptorModelProperty(OperationElement operationMethod) {
    this.operationElement = operationMethod;
  }

  public OperationElement getOperationElement() {
    return operationElement;
  }

  @Override
  public String getName() {
    return "operation-method-element";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
