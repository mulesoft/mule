/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
