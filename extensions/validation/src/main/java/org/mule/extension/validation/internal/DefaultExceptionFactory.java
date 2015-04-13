/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.reflections.ReflectionUtils.getConstructors;
import static org.reflections.ReflectionUtils.withParameters;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.extension.validation.api.ExceptionFactory;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Default implementation of {@link ExceptionFactory}.
 * <p/>
 * In order to create the {@link Exception} instances, it scans
 * the requested {@link Class}es for a public {@link Constructor}
 * which receives two arguments assignable from {@link ValidationResult}
 * and {@link MuleEvent}. If it's not found, a new search is done
 * looking for a constructor with a single argument assignable from
 * {@link ValidationResult}. If such {@link Constructor} is not found
 * then it tries with a default one. If it's still not found then
 * an {@link IllegalArgumentException} exception is thrown because
 * the requested {@link Class} does not comply with the rules of this
 * factory. For performance reasons, the {@link Constructor} chosen
 * for each class are cached.
 *
 * @see ExceptionFactory
 * @since 3.7.0
 */
public class DefaultExceptionFactory implements ExceptionFactory
{

    private final LoadingCache<Class<? extends Exception>, ConstructorDelegate<? extends Exception>> constructorCache = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends Exception>, ConstructorDelegate<? extends Exception>>()
    {
        @Override
        public ConstructorDelegate<? extends Exception> load(Class<? extends Exception> exceptionType) throws Exception
        {
            return selectMostCompleteConstructor(exceptionType);
        }
    });

    private final LoadingCache<String, Class<? extends Exception>> classCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Class<? extends Exception>>()
    {
        @Override
        public Class<? extends Exception> load(String exceptionClassName) throws Exception
        {
            Class<? extends Exception> exceptionClass;
            try
            {
                exceptionClass = ClassUtils.getClass(exceptionClassName);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalArgumentException("Could not find exception class " + exceptionClassName);
            }

            if (!Exception.class.isAssignableFrom(exceptionClass))
            {
                throw new IllegalArgumentException(String.format(
                        "Was expecting an exception type, %s found instead", exceptionClass.getCanonicalName()));
            }

            return exceptionClass;
        }
    });

    @Override
    public <T extends Exception> T createException(ValidationResult result, Class<T> exceptionClass, MuleEvent event)
    {
        ConstructorDelegate<T> constructorDelegate = (ConstructorDelegate<T>) get(constructorCache, exceptionClass);
        try
        {
            return constructorDelegate.createException(result, result.getMessage(), event);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage(
                    String.format("Could not create exception of type %s. Exception message was:\n%s",
                                  exceptionClass.getName(),
                                  result.getMessage())));
        }
    }

    @Override
    public Exception createException(ValidationResult result, String exceptionClassName, MuleEvent event)
    {
        return createException(result, get(classCache, exceptionClassName), event);
    }

    private <K, V> V get(LoadingCache<K, V> cache, K key)
    {
        try
        {
            return cache.get(key);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof IllegalArgumentException)
            {
                throw (IllegalArgumentException) e.getCause();
            }

            throw new MuleRuntimeException(e);
        }
    }

    private <T extends Exception> ConstructorDelegate<T> selectMostCompleteConstructor(Class<T> exceptionType)
    {
        Collection<Constructor> candidate = getConstructors(exceptionType, withParameters(ValidationResult.class, MuleEvent.class));
        if (!CollectionUtils.isEmpty(candidate))
        {
            return new ValidationResultAndEventConstructorDelegate<>((Constructor<T>) candidate.iterator().next());
        }

        candidate = getConstructors(exceptionType, withParameters(ValidationResult.class));
        if (!CollectionUtils.isEmpty(candidate))
        {
            return new ValidationResultConstructorDelegate((Constructor<T>) candidate.iterator().next());
        }

        candidate = getConstructors(exceptionType, withParameters(String.class));

        if (CollectionUtils.isEmpty(candidate))
        {
            throw new IllegalArgumentException(
                    String.format(
                            "Exception type %s was expected to contain at least one accessible constructor with eia single String argument but a matching constructor " +
                            "could not be found.",
                            exceptionType.getCanonicalName()));
        }

        return new DirectMessageConstructorDelegate((Constructor<T>) candidate.iterator().next());
    }

    private interface ConstructorDelegate<T extends Exception>
    {

        T createException(ValidationResult validationResult, String message, MuleEvent event) throws Exception;
    }

    private class ValidationResultAndEventConstructorDelegate<T extends Exception> implements ConstructorDelegate<T> {

        private final Constructor<T> constructor;

        public ValidationResultAndEventConstructorDelegate(Constructor<T> constructor)
        {
            this.constructor = constructor;
        }

        @Override
        public T createException(ValidationResult validationResult, String message, MuleEvent event) throws Exception
        {
            return constructor.newInstance(validationResult, event);
        }
    }

    private class ValidationResultConstructorDelegate<T extends Exception> implements ConstructorDelegate<T>
    {

        private final Constructor<T> constructor;

        private ValidationResultConstructorDelegate(Constructor<T> constructor)
        {
            this.constructor = constructor;
        }

        @Override
        public T createException(ValidationResult validationResult, String message, MuleEvent event) throws Exception
        {
            return constructor.newInstance(validationResult);
        }
    }

    private class DirectMessageConstructorDelegate<T extends Exception> implements ConstructorDelegate<T>
    {

        private final Constructor<T> constructor;

        public DirectMessageConstructorDelegate(Constructor<T> constructor)
        {
            this.constructor = constructor;
        }

        @Override
        public T createException(ValidationResult validationResult, String message, MuleEvent event) throws Exception
        {
            return constructor.newInstance(message);
        }
    }
}
