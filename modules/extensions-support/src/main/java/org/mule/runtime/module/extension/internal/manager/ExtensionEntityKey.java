/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
