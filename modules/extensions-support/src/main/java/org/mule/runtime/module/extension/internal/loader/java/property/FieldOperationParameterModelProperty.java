/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;

public class FieldOperationParameterModelProperty implements ModelProperty {

  @Override
  public String getName() {
    return "fieldOperationParameter";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
