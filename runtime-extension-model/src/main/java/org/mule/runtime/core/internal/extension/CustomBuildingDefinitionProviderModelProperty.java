/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
