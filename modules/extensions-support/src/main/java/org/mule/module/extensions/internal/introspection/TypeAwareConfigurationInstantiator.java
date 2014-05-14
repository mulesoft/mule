/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extensions.internal.util.IntrospectionUtils.checkInstantiable;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleRuntimeException;
import org.mule.extensions.introspection.ConfigurationInstantiator;

/**
 * Implementation of {@link ConfigurationInstantiator} which creates instances
 * based on a given {@link Class} which is assumed to have a default and public
 * constructor.
 *
 * @since 3.7.0
 */
final class TypeAwareConfigurationInstantiator implements ConfigurationInstantiator
{

    private final Class<?> configurationType;

    /**
     * Constructor which receives the type to be instantiated on each invocation to
     * {@link #newInstance()}. This type must be not {@code null}, and have a public default
     * constructor. Otherwise an {@link IllegalArgumentException} is thrown
     *
     * @param configurationType the type to be instantiated
     * @throws IllegalArgumentException if the type is {@code null} or doesn't have a default public constructor
     */
    TypeAwareConfigurationInstantiator(Class<?> configurationType)
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
