/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;

/**
 * {@link ParameterModelParser} implementation for Mule SDK (optional parameters).
 *
 * @since 4.5.0
 */
public class MuleSdkOptionalParameterModelParser extends MuleSdkParameterModelParser implements ParameterModelParser {

  private Object defaultValue = null;

  public MuleSdkOptionalParameterModelParser(ComponentAst parameter, TypeLoader typeLoader,
                                             ExtensionModelHelper extensionModelHelper) {
    super(parameter, typeLoader, extensionModelHelper);

    parseStructure();
  }

  private void parseStructure() {
    defaultValue = getOptionalParameter(parameterAst, "defaultValue").orElse(null);
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }
}
