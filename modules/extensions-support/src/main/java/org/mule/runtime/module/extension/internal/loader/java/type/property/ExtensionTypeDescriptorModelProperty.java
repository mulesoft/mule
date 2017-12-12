/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;


/**
 * Binds an Extension, Source, Connection Provider or Config with a {@link Type}
 *
 * @since 4.1
 */
public class ExtensionTypeDescriptorModelProperty implements ModelProperty {

  private Type type;

  public ExtensionTypeDescriptorModelProperty(Type type) {
    this.type = type;
  }

  @Override
  public String getName() {
    return "extension-type-descriptor";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  public Type getType() {
    return type;
  }
}
