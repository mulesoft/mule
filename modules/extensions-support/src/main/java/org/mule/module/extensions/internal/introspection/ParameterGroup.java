/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Capable;
import org.mule.extensions.introspection.Parameter;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.module.extensions.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extensions.internal.util.CapabilityUtils;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A metadata class that groups a set of parameters together.
 * It caches reflection objects necessary for handling those parameters
 * so that introspection is not executed every time, resulting in a performance gain.
 * <p/>
 * Because groups can be nested, this class also implements {@link Capable},
 * allowing for this group to have a {@link ParameterGroupCapability} which
 * describes the nested group.
 * <p/>
 * To decouple this class from the representation model (which depending on the
 * context could be a {@link Declaration} or an actual {@link Parameter}, this class
 * references parameters by name
 *
 * @since 3.7.0
 */
public class ParameterGroup implements Capable
{

    /**
     * The type of the pojo which implements the group
     */
    private final Class<?> type;

    /**
     * The setter that allows assigning the group of type
     * {@link #type} into its owner.
     */
    private final Method setter;

    /**
     * A {@link Map} in which the keys are parameter names
     * and the values are their corresponding setter methods
     */
    private final Map<String, Method> parameters = new HashMap<>();

    /**
     * The capabilities set
     */
    private Set<Object> capabilities = new HashSet<>();


    public ParameterGroup(Class<?> type, Method setter)
    {
        checkArgument(type != null, "field cannot be null");
        checkArgument(setter != null, "setter cannot be null");

        this.type = type;
        this.setter = setter;
    }

    /**
     * Adds a parameter to the group
     *
     * @param name   the name of the parameter
     * @param method the parameter's setter
     * @return {@value this}
     */
    public ParameterGroup addParameter(String name, Method method)
    {
        parameters.put(name, method);
        return this;
    }

    public Class<?> getType()
    {
        return type;
    }

    public Method getSetter()
    {
        return setter;
    }

    public Map<String, Method> getParameters()
    {
        return ImmutableMap.copyOf(parameters);
    }

    public void addCapability(Object capability)
    {
        checkArgument(capability != null, "cannot add a null capability");
        capabilities.add(capability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<T> getCapabilities(Class<T> capabilityType)
    {
        return CapabilityUtils.getCapabilities(capabilities, capabilityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCapableOf(Class<?> capabilityType)
    {
        return CapabilityUtils.isCapableOf(capabilities, capabilityType);
    }
}
