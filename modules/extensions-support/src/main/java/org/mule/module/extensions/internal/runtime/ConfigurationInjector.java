/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import static org.mule.module.extensions.internal.util.IntrospectionUtils.getFieldsAnnotatedWith;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.extensions.annotations.WithConfig;
import org.mule.extensions.introspection.Configuration;

import java.lang.reflect.Field;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

/**
 * A configuration injector takes an instance of a class which implements
 * an extension operation and injects the configuration that it should use.
 * <p/>
 * The configuration is injected into a field annotated with {@link WithConfig}.
 * The reason for that particular annotation to exists instead of using
 * the traditional {@link Inject} is explained on {@link WithConfig}'s javadoc.
 * <p/>
 * It's very important to notice that what will be injected is not an instance of
 * {@link Configuration} but an object which complies with the model described in
 * a {@link Configuration}.
 * <p/>
 * Instances of this class are to be created through the {@link #of(Class)} factory method. That method
 * will perform introspection of the type in which configs will be injected in order to locate the target field.
 * If there's more than one annotated field (in the actual class or any of its ancestors),
 * then an {@link IllegalArgumentException} is thrown. Superclasses are also scanned.
 * <p/>
 * Finally, notice that the operation class might not have a need for the configuration and
 * thus have no field annotated. In that case, nothing is injected.
 *
 * @param <C> the type of the configuration objects to be injected
 * @param <T> the type of the operation objects in which configurations are to be injected
 * @since 3.7.0
 */
final class ConfigurationInjector<C, T>
{

    /**
     * Creates a new {@link ConfigurationInjector} capable of injecting configurations
     * into instances of {@code injectableType}.
     *
     * @param injectableType the type to receive injections
     * @param <C>            the type of the configurations to be injected
     * @param <T>            the type in which configurations are to be injected
     * @return a {@link ConfigurationInjector}
     * @throws IllegalArgumentException if more than one field annotated with {@link WithConfig} is found
     */
    static <C, T> ConfigurationInjector<C, T> of(Class<T> injectableType)
    {
        Set<Field> fields = getFieldsAnnotatedWith(injectableType, WithConfig.class);

        ConfigurationInjector<C, T> injector = new ConfigurationInjector<>();

        if (CollectionUtils.isEmpty(fields))
        {
            injector.delegate = new NullDelegate<>();
        }
        else
        {
            checkArgument(fields.size() == 1, String.format("Type %s has more than one field annotated with %s",
                                                            injectableType.getName(), WithConfig.class.getName()));

            injector.delegate = new FieldInjector<>(fields.iterator().next());
        }

        return injector;
    }

    private Delegate<C, T> delegate;

    private ConfigurationInjector()
    {
    }

    /**
     * If necessary, injects {@code configuration} into {@code instance}
     *
     * @param configuration the configuration to be used
     * @param instance      the object that implements the operation
     */
    void injectConfiguration(C configuration, T instance)
    {
        delegate.injectConfiguration(configuration, instance);
    }

    private interface Delegate<C, T>
    {

        void injectConfiguration(C configuration, T instance);
    }

    private static class FieldInjector<C, T> implements Delegate<C, T>
    {

        private final Field field;

        private FieldInjector(Field field)
        {
            checkArgument(field != null, "Field cannot be null");
            this.field = field;
            field.setAccessible(true);
        }

        @Override
        public void injectConfiguration(C configuration, T instance)
        {
            try
            {
                field.set(instance, configuration);
            }
            catch (IllegalAccessException e)
            {
                throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not set value on field"), e);
            }
        }
    }

    private static class NullDelegate<C, T> implements Delegate<C, T>
    {

        private NullDelegate()
        {
        }

        @Override
        public void injectConfiguration(Object configuration, Object instance)
        {
        }
    }
}
