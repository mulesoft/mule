/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.extension;

import static org.mule.runtime.api.meta.Category.COMMUNITY;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

public class TestExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  private static final String EXTENSION_NAME = "crafted-extension";

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    extensionDeclarer.named(EXTENSION_NAME)
        .describedAs("Crafted Extension")
        .onVersion("1.0.0")
        .withCategory(COMMUNITY)
        .fromVendor("Mulesoft");
  }
}
