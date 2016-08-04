/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link TypeBasedComponent} specification for classes that are considered as Extensions
 *
 * @since 4.0
 */
public class ExtensionType<T> extends TypeBasedComponent<T> implements WithParameters
{

    public ExtensionType(Class<T> aClass)
    {
        super(aClass);
    }

    /**
     * @return A list {@link TypeBasedComponent} of declared configurations
     */
    public List<TypeBasedComponent<?>> getConfigurations()
    {
        final Optional<Configurations> optionalConfigurations = this.getAnnotation(Configurations.class);
        if (optionalConfigurations.isPresent())
        {
            final Configurations configurations = optionalConfigurations.get();
            return Stream
                    .of(configurations.value())
                    .map(TypeBasedComponent::new)
                    .collect(toList());
        }
        return emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionParameter> getParameters()
    {
        return ImmutableList.<ExtensionParameter>builder()
                .addAll(getAnnotatedFields(Parameter.class))
                .addAll(getAnnotatedFields(ParameterGroup.class)).build();
    }
}
