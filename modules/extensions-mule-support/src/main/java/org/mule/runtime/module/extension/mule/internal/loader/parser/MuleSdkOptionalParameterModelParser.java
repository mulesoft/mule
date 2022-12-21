/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;

import java.util.Optional;

import static java.util.Optional.of;

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

  @Override
  public Optional<MuleVersion> getMinMuleVersion() {
    return of(new MuleVersion("4.5"));
  }
}
