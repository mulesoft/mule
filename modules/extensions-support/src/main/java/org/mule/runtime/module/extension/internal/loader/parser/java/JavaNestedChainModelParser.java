/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.parser.NestedChainModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link NestedChainModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
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

  @Override
  public List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory) {
    return JavaStereotypeModelParserUtils.getAllowedStereotypes(extensionParameter, extensionParameter.getType(), factory);
  }

  @Override
  public Set<String> getSemanticTerms() {
    return new LinkedHashSet<>();
  }
}
