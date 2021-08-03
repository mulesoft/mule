/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;

public class JavaNestedChainModelParser implements NestedChainModelParser {

  private final ExtensionParameter extensionParameter;

  public JavaNestedChainModelParser(ExtensionParameter extensionParameter) {
    this.extensionParameter = extensionParameter;
  }

  @Override
  public String getName() {
    return extensionParameter.getAlias();
  }

  @Override
  public String getDescription() {
    return extensionParameter.getDescription();
  }

  @Override
  public boolean isRequired() {
    return extensionParameter.isRequired();
  }
}
