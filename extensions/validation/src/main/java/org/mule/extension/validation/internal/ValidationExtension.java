/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NamedObject;
import org.mule.api.config.Config;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.extension.annotations.Extensible;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.Operations;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.capability.Xml;
import org.mule.extension.annotations.param.Optional;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Operation;
import org.mule.extension.validation.api.ExceptionFactory;
import org.mule.extension.validation.api.Validator;
import org.mule.util.ObjectNameHelper;

import java.util.Locale;

/**
 * An extension which provides validation capabilities by exposing a series of
 * {@link Validator}s as {@link org.mule.extension.introspection.Extension} {@link Operation}s
 *
 * This class not only defines the extension but also acts as the only available {@link Configuration}
 * for it. It allows parametrizing the {@link Validator}s with custom {@link ExceptionFactory} and
 * i18n bundles (through a {@link I18NConfig}
 *
 * THe configured {@link ExceptionFactory} (either user provided or default) is registered into the
 * {@link MuleRegistry} allowing it to participate on the mule lifecycle.
 *
 * @since 3.7.0
 */
@Extension(name = "validation", description = "Mule Validation Extension", version = "3.7")
@Operations({CommonValidationOperations.class, CustomValidatorOperation.class, ValidationStrategies.class, NumberValidationOperation.class})
@Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/validation", namespace = "validation", schemaVersion = "3.7")
@Extensible(alias = "validator-message-processor")
public class ValidationExtension extends AbstractAnnotatedObject implements Config, NamedObject, Initialisable, MuleContextAware
{

    public static final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();
    private static final String EXCEPTION_FACTORY_PARAMETER_NAME = "exceptionFactory";

    private ValidationMessages messageFactory;
    private ExceptionFactory exceptionFactory;
    private MuleContext muleContext;

    @Parameter(alias = EXCEPTION_FACTORY_PARAMETER_NAME)
    @Optional
    private ExceptionFactorySource exceptionFactorySource;

    @Parameter(alias = "i18n")
    @Optional
    private I18NConfig i18nConfig;


    @Override
    public void initialise() throws InitialisationException
    {
        initialiseExceptionFactory();
        initialiseMessageFactory();
    }

    private void initialiseMessageFactory()
    {
        if (i18nConfig == null)
        {
            messageFactory = new ValidationMessages();
        }
        else
        {
            messageFactory = new ValidationMessages(i18nConfig.getBundlePath(), i18nConfig.getLocale());
        }
    }

    private void initialiseExceptionFactory() throws InitialisationException
    {
        if (exceptionFactorySource == null)
        {
            exceptionFactory = new DefaultExceptionFactory();
        }
        else
        {
            try
            {
                exceptionFactory = exceptionFactorySource.getObject(muleContext);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }

        try
        {
            ObjectNameHelper objectNameHelper = new ObjectNameHelper(muleContext);
            muleContext.getRegistry().registerObject(objectNameHelper.getUniqueName(EXCEPTION_FACTORY_PARAMETER_NAME), exceptionFactory);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not register ExceptionFactory of class " + exceptionFactory.getClass().getName()), e);
        }
    }

    public ValidationMessages getMessageFactory()
    {
        return messageFactory;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public ExceptionFactory getExceptionFactory()
    {
        return exceptionFactory;
    }

    @Override
    public String getName()
    {
        return "Validation";
    }
}
