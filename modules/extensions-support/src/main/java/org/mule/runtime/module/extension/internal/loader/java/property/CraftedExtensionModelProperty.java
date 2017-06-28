/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

/**
 * Marker {@link ModelProperty} to indicate if the current {@link ExtensionModel} must NOT be registered to {@link ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 */
public class CraftedExtensionModelProperty implements ModelProperty {

  @Override
  public String getName() {
    return "craftedExtensionModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

}
