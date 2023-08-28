/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Marker {@link ModelProperty} to communicate that the Extension model is being generated during compile time instead of being a
 * runtime model loading.
 *
 * @since 4.1
 */
public class CompileTimeModelProperty implements ModelProperty {

  @Override
  public String getName() {
    return "isCompileTime";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
