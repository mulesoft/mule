/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.MediaType;
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

  @MediaType(TEXT_PLAIN)
  public String getString(@Config ImplicitConfigWithOptionalParameter config) {
    return optionalParameterValueAtInitialize;
  }

}
