/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * {@link ModelProperty} to be used when an {@link ExtensionModel} should not generate parsers for the extension operations.
 * 
 * @since 4.0
 */
public class CustomBuildingDefinitionProviderModelProperty implements ModelProperty {

  @Override
  public String getName() {
    return "customBuildingDefinition";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
