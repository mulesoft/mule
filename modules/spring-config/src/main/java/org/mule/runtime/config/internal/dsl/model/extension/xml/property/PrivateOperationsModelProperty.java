/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;

import java.util.List;
import java.util.Optional;

/**
 * Model property responsible of holding private <operation/>s defined within a <module/> that will never be called, neither
 * exposed, from outside of it.
 *
 * @since 4.2
 */
public class PrivateOperationsModelProperty implements ModelProperty {

  private List<OperationModel> privateOperationsModels;

  public PrivateOperationsModelProperty(List<OperationModel> privateOperationsModels) {
    this.privateOperationsModels = privateOperationsModels;
  }

  /**
   * Returns the {@link OperationModel} that matches
   * the given name.
   *
   * @param name case sensitive operation name
   * @return an {@link Optional} {@link OperationModel}
   */
  public Optional<OperationModel> getOperationModel(String name) {
    return privateOperationsModels
        .stream()
        .filter(operationModel -> operationModel.getName().equals(name))
        .findFirst();
  }

  @Override
  public String getName() {
    return "privateOperationsModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
