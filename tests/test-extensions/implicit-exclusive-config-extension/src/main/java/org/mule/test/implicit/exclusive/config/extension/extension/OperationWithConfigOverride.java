/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class OperationWithConfigOverride implements Initialisable {

  private String optionalParameterValueAtInitialize;

  @Parameter
  @ConfigOverride
  private String optionalWithStaticDefault = "Some random default!";

  @Override
  public void initialise() throws InitialisationException {
    if (optionalWithStaticDefault == null) {
      throw new InitialisationException(createStaticMessage("optionalWithStaticDefault is not permitted to be null!"), this);
    }
    optionalParameterValueAtInitialize = optionalWithStaticDefault;
  }

  public String getString(@Config ImplicitConfigWithOptionalParameter config) {
    return optionalParameterValueAtInitialize;
  }

}
