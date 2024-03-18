/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.extension;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Indicates that the declaration with this property may not appear more than once in an application or config file.
 *
 * @since 4.4
 */
public class SingletonModelProperty implements ModelProperty {

  private static final long serialVersionUID = 8934635188400684630L;

  private final boolean appliesToFile;

  public SingletonModelProperty(boolean appliesToFile) {
    this.appliesToFile = appliesToFile;
  }

  @Override
  public String getName() {
    return "singleton";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  public boolean isAppliesToFile() {
    return appliesToFile;
  }
}
