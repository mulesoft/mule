/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.extension.api.introspection.ExpressionSupport.REQUIRED;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.Described;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.InterceptableModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.ParametrizedModel;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.extension.api.runtime.InterceptorFactory;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for handling {@link ExtensionModel extensions}
 *
 * @since 3.7.0
 */
public class MuleExtensionUtils
{

    private MuleExtensionUtils()
    {
    }

    /**
     * Returns a {@link Map} in which the keys are the {@link Described#getName() names} of the items
     * in the {@code objects} {@link List}, and the values are the items themselves.
     *
     * @param objects a {@link List} with items that implement the {@link Described} interface
     * @param <T>     the generic type of the items in {@code objects}
     * @return a {@link Map} in which the keys are the item's names and the values are the items
     */
    public static <T extends Described> Map<String, T> toMap(List<T> objects)
    {
        ImmutableMap.Builder<String, T> map = ImmutableMap.builder();
        for (T object : objects)
        {
            map.put(object.getName(), object);
        }

        return map.build();
    }

    /**
     * Returns {@code true} if any of the items in {@code resolvers} return true for the
     * {@link ValueResolver#isDynamic()} method
     *
     * @param resolvers a {@link Iterable} with instances of {@link ValueResolver}
     * @param <T>       the generic type of the {@link ValueResolver} items
     * @return {@code true} if at least one {@link ValueResolver} is dynamic, {@code false} otherwise
     */
    public static <T extends Object> boolean hasAnyDynamic(Iterable<ValueResolver<T>> resolvers)
    {
        for (ValueResolver resolver : resolvers)
        {
            if (resolver.isDynamic())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the {@link ParameterModel parameters} from {@code model} which
     * supports or requires expressions
     *
     * @param model a {@link ParametrizedModel}
     * @return a {@link List} of {@link ParameterModel}. Can be empty but will never be {@code null}
     */
    public static List<ParameterModel> getDynamicParameters(ParametrizedModel model)
    {
        return model.getParameterModels().stream()
                .filter(parameter -> acceptsExpressions(parameter.getExpressionSupport()))
                .collect(toList());
    }

    /**
     * @param support a {@link ExpressionSupport}
     * @return Whether or not the given {@code support} is one which accepts or requires expressions
     */
    public static boolean acceptsExpressions(ExpressionSupport support)
    {
        return support == SUPPORTED || support == REQUIRED;
    }

    /**
     * Returns a {@link List} with all the {@link OperationModel} in the {@code extensionModel}
     * which require a connection.
     *
     * @param extensionModel a {@link ExtensionModel}
     * @return a {@link List} of {@link OperationModel}. It might be empty but will never be {@code null}
     */
    public static List<OperationModel> getConnectedOperations(ExtensionModel extensionModel)
    {
        return extensionModel.getOperationModels().stream()
                .filter(o -> o.getModelProperty(ConnectionTypeModelProperty.KEY) != null)
                .collect(toList());
    }

    public static Class<?> getOperationsConnectionType(ExtensionModel extensionModel)
    {
        Set<Class<?>> connectionTypes = extensionModel.getOperationModels().stream()
                .map(operation -> {
                    ConnectionTypeModelProperty connectionProperty = operation.getModelProperty(ConnectionTypeModelProperty.KEY);
                    return connectionProperty != null ? connectionProperty.getConnectionType() : null;
                })
                .filter(type -> type != null)
                .collect(toSet());

        if (isEmpty(connectionTypes))
        {
            return null;
        }
        else if (connectionTypes.size() > 1)
        {
            throw new IllegalModelDefinitionException(String.format("Extension '%s' has operation which require connections of different types ([%s]). " +
                                                                    "Please standarize on one single connection type to ensure that all operations work with any compatible %s",
                                                                    extensionModel.getName(), Joiner.on(", ").join(connectionTypes), ConnectionProvider.class.getSimpleName()));
        }
        else
        {
            return connectionTypes.stream().findFirst().get();
        }
    }

    /**
     * Sorts the given {@code list} in ascending alphabetic order, using {@link Described#getName()}
     * as the sorting criteria
     *
     * @param list a {@link List} with instances of {@link Described}
     * @param <T>  the generic type of the items in the {@code list}
     * @return the sorted {@code list}
     */
    public static <T extends Described> List<T> alphaSortDescribedList(List<T> list)
    {
        if (isEmpty(list))
        {
            return list;
        }

        Collections.sort(list, new DescribedComparator());
        return list;
    }

    /**
     * Creates a new {@link List} of {@link Interceptor interceptors} using the
     * factories returned by {@link InterceptableModel#getInterceptorFactories()}
     *
     * @param model the model on which {@link InterceptableModel#getInterceptorFactories()} is to be invoked
     * @return an immutable {@link List} with instances of {@link Interceptor}
     */
    public static List<Interceptor> createInterceptors(InterceptableModel model)
    {
        return createInterceptors(model.getInterceptorFactories());
    }

    /**
     * Creates a new {@link List} of {@link Interceptor interceptors} using the
     * {@code interceptorFactories}
     *
     * @param interceptorFactories a {@link List} with instances of {@link InterceptorFactory}
     * @return an immutable {@link List} with instances of {@link Interceptor}
     */
    public static List<Interceptor> createInterceptors(List<InterceptorFactory> interceptorFactories)
    {
        if (isEmpty(interceptorFactories))
        {
            return ImmutableList.of();
        }

        return interceptorFactories.stream()
                .map(InterceptorFactory::createInterceptor)
                .collect(new ImmutableListCollector<>());
    }

    /**
     * Returns the value of {@link ImplementingTypeModelProperty#getType()} if the {@code model}
     * is enriched with such property
     *
     * @param model the model presumed to be enriched
     * @param <T>   the generic type of the returned type
     * @return a {@link Class} or {@code null} if the {@code model} is not enriched with that property
     */
    public static <T> Class<T> getImplementingType(EnrichableModel model)
    {
        ImplementingTypeModelProperty property = model.getModelProperty(ImplementingTypeModelProperty.KEY);
        return property != null ? (Class<T>) property.getType() : null;
    }

    public static String getDefaultValue(Optional optional)
    {
        if (optional == null)
        {
            return null;
        }

        String defaultValue = optional.defaultValue();
        return Optional.NULL.equals(defaultValue) ? null : defaultValue;
    }

    public static MuleEvent getInitialiserEvent(MuleContext muleContext)
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
    }

    /**
     * Returns the {@link Method} that was used to declare the given
     * {@code operationDeclaration}.
     *
     * @param operationDeclaration a {@link OperationDeclaration}
     * @return A {@link Method} or {@code null} if the {@code operationDeclaration} was defined by other means
     */
    public static Method getImplementingMethod(OperationDeclaration operationDeclaration)
    {
        ImplementingMethodModelProperty methodProperty = operationDeclaration.getModelProperty(ImplementingMethodModelProperty.KEY);
        if (methodProperty != null)
        {
            return methodProperty.getMethod();
        }

        return null;
    }

    private static class DescribedComparator implements Comparator<Described>
    {

        @Override
        public int compare(Described o1, Described o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
