/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;

public final class TestExtensionSchemagenerator implements ExtensionSchemaGenerator {

  @Override
  public String generate(ExtensionModel extensionModel, DslResolvingContext context) {
    return "";
  }

  @Override
  public String generate(ExtensionModel extensionModel, DslResolvingContext context, DslSyntaxResolver dsl) {
    return "";
  }
}
