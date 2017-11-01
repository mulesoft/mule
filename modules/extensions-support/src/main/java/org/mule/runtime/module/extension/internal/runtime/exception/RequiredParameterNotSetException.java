/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;

/**
 * {@link MuleRuntimeException} that aims to be thrown when a required parameter is was set.
 *
 * @since 4.0
 */
public class RequiredParameterNotSetException extends MuleRuntimeException {

  private final String paramName;

  public RequiredParameterNotSetException(ParameterModel parameterModel) {
    this(parameterModel.getName());
  }

  public RequiredParameterNotSetException(String name) {
    super(createStaticMessage(format("Parameter '%s' is required but was not found", hyphenize(name))));
    this.paramName = name;
  }

  public String getParameterName() {
    return paramName;
  }
}
