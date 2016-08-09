/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import org.mule.runtime.extension.api.annotation.Extension;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class is the key that represents an Extension in the {@link ExtensionRegistry} The key is composed by the Extension name
 * and vendor.
 *
 * @since 4.0
 */
final class ExtensionEntityKey {

  private final String name;
  private final String vendor;

  protected ExtensionEntityKey(String name, String vendor) {
    this.vendor = vendor == null ? Extension.MULESOFT : vendor;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getVendor() {
    return vendor;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ExtensionEntityKey) {
      ExtensionEntityKey entity = (ExtensionEntityKey) o;
      return name.equals(entity.name) && vendor.equals(entity.vendor);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(vendor).append(name).toHashCode();
  }

}
