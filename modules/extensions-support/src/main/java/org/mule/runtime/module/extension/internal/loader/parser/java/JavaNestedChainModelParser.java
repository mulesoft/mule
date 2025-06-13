/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.module.extension.internal.loader.parser.java.route.JavaChainParsingUtils.parseChainExecutionOccurrence;

import org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.extension.api.loader.parser.NestedChainModelParser;
import org.mule.runtime.extension.api.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils;
import org.mule.sdk.api.annotation.route.ExecutionOccurrence;
import org.mule.sdk.api.runtime.route.Chain;

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
  private final boolean sdkApiDefined;

  public JavaNestedChainModelParser(ExtensionParameter extensionParameter) {
    this.extensionParameter = extensionParameter;
    sdkApiDefined = extensionParameter.getType().getDeclaringClass()
        .map(Chain.class::isAssignableFrom)
        .orElse(false);
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
  public ChainExecutionOccurrence getExecutionOccurrence() {
    return parseChainExecutionOccurrence(extensionParameter.getValueFromAnnotation(ExecutionOccurrence.class));
  }

  @Override
  public Set<String> getSemanticTerms() {
    return new LinkedHashSet<>();
  }

  @Override
  public boolean isSdkApiDefined() {
    return sdkApiDefined;
  }
}
