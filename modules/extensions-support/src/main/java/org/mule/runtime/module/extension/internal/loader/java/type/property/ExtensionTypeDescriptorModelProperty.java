/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.Type;


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
