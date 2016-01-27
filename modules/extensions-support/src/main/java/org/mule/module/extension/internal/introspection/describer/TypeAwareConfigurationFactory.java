/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.api.introspection.ConfigurationFactory;

/**
 * Implementation of {@link ConfigurationFactory} which creates instances
 * based on a given {@link Class} which is assumed to have a default and public
 * constructor.
 *
 * @since 3.7.0
 */
final class TypeAwareConfigurationFactory implements ConfigurationFactory
{

    private final Class<?> configurationType;

    /**
     * Creates an instance of a given {@code configurationType} on each invocation to
     * {@link #newInstance()}.
     *
     * @param configurationType the type to be instantiated. Must be not {@code null}, and have a public default constructor
     * @throws IllegalArgumentException if the type is {@code null} or doesn't have a default public constructor
     */
    TypeAwareConfigurationFactory(Class<?> configurationType)
    {
        checkArgument(configurationType != null, "configuration type cannot be null");
        checkInstantiable(configurationType);
        this.configurationType = configurationType;
    }

    /**
     * Returns a new instance on each invocation
     * {@inheritDoc}
     */
    @Override
    public Object newInstance()
    {
        try
        {
            return configurationType.newInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not instantiate configuration of type " + configurationType.getName()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getObjectType()
    {
        return configurationType;
    }
}
