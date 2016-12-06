/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import org.mule.extension.validation.internal.CommonValidationOperations;
import org.mule.extension.validation.internal.CustomValidatorOperation;
import org.mule.extension.validation.internal.NumberValidationOperation;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.extension.validation.internal.ValidationStrategies;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Locale;

/**
 * An extension which provides validation capabilities by exposing a series of {@link Validator}s as {@link ExtensionModel}
 * {@link OperationModel}s
 *
 * This class not only defines the extension but also acts as the only available {@link ConfigurationModel} for it. It allows
 * parametrizing the {@link Validator}s with i18n bundles (through a {@link I18NConfig}
 *
 * @since 3.7.0
 */
@Extension(name = "Validation",
    description = "Allows performing validations and throw an Exception if the validation fails")
@Operations({CommonValidationOperations.class, CustomValidatorOperation.class, ValidationStrategies.class,
    NumberValidationOperation.class})
@Extensible(alias = "validator-message-processor")
@Export(
    resources = {"/META-INF/services/org/mule/runtime/core/i18n/validation-messages.properties"})
@ErrorTypes(ValidationErrorTypes.class)
@OnException(ValidationExceptionEnricher.class)
@Throws(ValidationErrorTypeProvider.class)
public class ValidationExtension implements Config, NamedObject, Initialisable {

  public static final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();

  private ValidationMessages messageFactory;

  @Parameter
  @Optional
  private I18NConfig i18n;

  @Override
  public void initialise() throws InitialisationException {
    initialiseMessageFactory();
  }

  private void initialiseMessageFactory() {
    if (i18n == null) {
      messageFactory = new ValidationMessages();
    } else {
      messageFactory = new ValidationMessages(i18n.getBundlePath(), i18n.getLocale());
    }
  }

  public ValidationMessages getMessageFactory() {
    return messageFactory;
  }

  @Override
  public String getName() {
    return "Validation";
  }
}
