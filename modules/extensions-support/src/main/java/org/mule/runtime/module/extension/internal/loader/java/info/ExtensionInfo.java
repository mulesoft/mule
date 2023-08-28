/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.info;

import org.mule.runtime.api.meta.Category;

/**
 * Simple bean which maps the info defined in the {@link org.mule.runtime.extension.api.annotation.Extension} or
 * {@link org.mule.sdk.api.annotation.Extension} annotations, so that consumers can decouple from which was used.
 *
 * @since 4.5.0
 */
public class ExtensionInfo {

  private final String name;
  private final String vendor;
  private final Category category;

  public ExtensionInfo(String name, String vendor, Category category) {
    this.name = name;
    this.vendor = vendor;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getVendor() {
    return vendor;
  }

  public Category getCategory() {
    return category;
  }
}
