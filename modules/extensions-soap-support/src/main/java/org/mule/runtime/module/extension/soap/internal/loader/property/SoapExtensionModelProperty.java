/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.loader.property;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * An immutable model property which indicates that the extension is a Soap Based one.
 *
 * @since 4.0
 */
public final class SoapExtensionModelProperty implements ModelProperty {

  private static final String NAME = "soapExtensionType";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }


  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
