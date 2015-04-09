/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.api.MuleEvent;
import org.mule.api.registry.MuleRegistry;
import org.mule.extension.annotations.Operation;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.introspection.declaration.Construct;
import org.mule.extension.validation.ObjectSource;
import org.mule.extension.validation.ValidationExtension;
import org.mule.extension.validation.api.Validator;

/**
 * Defines a stateful operation of {@link ValidationExtension} which
 * is capable of executing custom validators provided by a
 * third party. The {@link Validator} can be provided via a {@link ObjectSource}
 * which means that the user could have specified either a classname or a
 * named reference to it.
 * <p/>
 * If the user provided a classname, then the {@link Class} that it represents
 * is expected to have a default public {@link Construct} which can be used to
 * instantiate it. Because the {@link Validator} api does not make any promises
 * around statefulness or thread safety, a new instance will be created on each
 * invokation.
 * <p/>
 * If the {@link Validator} is provided via a reference, then a lookup
 * to the {@link MuleRegistry} will be performed. Again, because registry
 * entries are mutable, a new lookup will be performed each time.
 *
 * @since 3.7.0
 */
public class CustomValidatorOperation
{

    private final ValidationExtension config;

    public CustomValidatorOperation(ValidationExtension config)
    {
        this.config = config;
    }

    @Operation
    public void customValidator(@ParameterGroup ObjectSource<Validator> validatorSource, MuleEvent event) throws Exception
    {
        validatorSource.getObject(event.getMuleContext()).validate(event);
    }
}
