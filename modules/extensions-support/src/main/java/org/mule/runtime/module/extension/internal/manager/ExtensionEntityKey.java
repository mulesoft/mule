/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

/**
 * This class is the key that represents an Extension in the {@link ExtensionRegistry} The key is composed by the Extension name
 * and vendor.
 *
 * @since 4.0
 */
final class ExtensionEntityKey {

  private final String name;

  protected ExtensionEntityKey(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ExtensionEntityKey && ((ExtensionEntityKey) o).getName().equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

}
