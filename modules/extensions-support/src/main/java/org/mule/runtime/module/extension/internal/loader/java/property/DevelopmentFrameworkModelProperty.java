/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.ExtensionDevelopmentFramework;

/**
 * {@link ModelProperty} to indicate the development framework used for developing the associated extension.
 * <p>
 * Note that some extensions may not have this property, meaning they were not developed by any of the main development
 * frameworks. An example of this could be extensions programmatically declared.
 *
 * @since 4.5, moved form extensions-api in 4.8
 */
public class DevelopmentFrameworkModelProperty implements ModelProperty {

  private static final long serialVersionUID = 1L;

  private final String id;

  public DevelopmentFrameworkModelProperty(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "extensionDevelopmentFramework";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return The ID associated with the development framework used for developing the associated extension.
   */
  public String getId() {
    return id;
  }
}
