/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.validation.internal.CommonValidationOperations;
import org.mule.extension.validation.internal.CustomValidatorOperation;
import org.mule.extension.validation.internal.DefaultExceptionFactory;
import org.mule.extension.validation.internal.NumberValidationOperation;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.extension.validation.internal.ValidationStrategies;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.ObjectNameHelper;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;

import java.util.Locale;

/**
 * An extension which provides validation capabilities by exposing a series of {@link Validator}s as {@link ExtensionModel}
 * {@link OperationModel}s
 *
 * This class not only defines the extension but also acts as the only available {@link ConfigurationModel} for it. It allows
 * parametrizing the {@link Validator}s with custom {@link ExceptionFactory} and i18n bundles (through a {@link I18NConfig}
 *
 * THe configured {@link ExceptionFactory} (either user provided or default) is registered into the {@link MuleRegistry} allowing
 * it to participate on the mule lifecycle.
 *
 * @since 3.7.0
 */
@Extension(name = "Validation Module",
    description = "Allows performing validations and throw an Exception if the validation fails")
@Operations({CommonValidationOperations.class, CustomValidatorOperation.class, ValidationStrategies.class,
    NumberValidationOperation.class})
@Extensible(alias = "validator-message-processor")
@Export(
    resources = {"/META-INF/services/org/mule/runtime/core/i18n/validation-messages.properties",
        "/META-INF/services/org/mule/runtime/core/config/registry-bootstrap.properties"},
    classes = {org.mule.extension.validation.api.el.ValidationElExtension.class})
public class ValidationExtension extends AbstractAnnotatedObject implements Config, NamedObject, Initialisable, MuleContextAware {

  public static final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();
  private static final String EXCEPTION_FACTORY_PARAMETER_NAME = "exceptionFactory";

  private ValidationMessages messageFactory;
  private ExceptionFactory exceptionFactory;
  private MuleContext muleContext;

  @Parameter
  @Alias(EXCEPTION_FACTORY_PARAMETER_NAME)
  @Optional
  private ExceptionFactorySource exceptionFactorySource;

  @Parameter
  @Optional
  private I18NConfig i18n;


  @Override
  public void initialise() throws InitialisationException {
    initialiseExceptionFactory();
    initialiseMessageFactory();
  }

  private void initialiseMessageFactory() {
    if (i18n == null) {
      messageFactory = new ValidationMessages();
    } else {
      messageFactory = new ValidationMessages(i18n.getBundlePath(), i18n.getLocale());
    }
  }

  private void initialiseExceptionFactory() throws InitialisationException {
    if (exceptionFactorySource == null) {
      exceptionFactory = new DefaultExceptionFactory();
    } else {
      try {
        exceptionFactory = exceptionFactorySource.getObject(muleContext);
      } catch (Exception e) {
        throw new InitialisationException(e, this);
      }
    }

    try {
      ObjectNameHelper objectNameHelper = new ObjectNameHelper(muleContext);
      muleContext.getRegistry().registerObject(objectNameHelper.getUniqueName(EXCEPTION_FACTORY_PARAMETER_NAME),
                                               exceptionFactory);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not register ExceptionFactory of class "
          + exceptionFactory.getClass().getName()), e);
    }
  }

  public ValidationMessages getMessageFactory() {
    return messageFactory;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public ExceptionFactory getExceptionFactory() {
    return exceptionFactory;
  }

  @Override
  public String getName() {
    return "Validation";
  }
}
