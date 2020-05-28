/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.extension;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Indicates that the declaration with this property may not appear more than once in an application.
 *
 * @since 4.4
 */
public class SingletonModelProperty implements ModelProperty {

  private static final long serialVersionUID = 8934635188400684630L;

  @Override
  public String getName() {
    return "singleton";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

}
