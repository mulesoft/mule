/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.utils;

import org.mule.api.expression.ExpressionManager;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.ParametrizedModel;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.List;

/**
 * Utilities for creating object with implicit values based on a {@link ParametrizedModel}
 *
 * @since 4.0
 */
public final class ImplicitObjectUtils
{

    private ImplicitObjectUtils()
    {
    }

    /**
     * Creates a {@link ResolverSet} based on the default values for the {@link ParameterModel}s
     * in the {@code parametrizedModel}.
     * <p>
     * If a {@link ParameterModel} returns {@code null} for {@link ParameterModel#getDefaultValue()}
     * then it's ignored
     *
     * @param parametrizedModel a model holding the {@link ParameterModel}s to consider
     * @param expressionManager a {@link ExpressionManager} for the default values which are expessions
     * @return a {@link ResolverSet}
     */
    public static ResolverSet buildImplicitResolverSet(ParametrizedModel parametrizedModel, ExpressionManager expressionManager)
    {
        ResolverSet resolverSet = new ResolverSet();
        for (ParameterModel parameterModel : parametrizedModel.getParameterModels())
        {
            Object defaultValue = parameterModel.getDefaultValue();
            if (defaultValue != null)
            {
                ValueResolver<Object> valueResolver;
                if (defaultValue instanceof String && expressionManager.isExpression((String) defaultValue) && parameterModel.getExpressionSupport() != ExpressionSupport.LITERAL)
                {
                    valueResolver = new TypeSafeExpressionValueResolver<>((String) defaultValue, parameterModel.getType());
                }
                else
                {
                    valueResolver = new StaticValueResolver<>(defaultValue);
                }

                resolverSet.add(parameterModel, valueResolver);
            }
        }

        return resolverSet;
    }

    /**
     * Returns the first item in the {@code models} {@link List} that can be used implicitly.
     * <p>
     * A {@link ParametrizedModel} is consider to be implicit when all its {@link ParameterModel}s
     * are either optional or have a default value
     *
     * @param models a {@link List} of {@code T}
     * @param <T>    the generic type of the items in the {@code models}. It's a type which is assignable from {@link ParametrizedModel}
     * @return one of the items in {@code models} or {@code null} if none of the models are implicit
     */
    public static <T extends ParametrizedModel> T getFirstImplicit(List<T> models)
    {
        for (T model : models)
        {
            if (canBeUsedImplicitly(model))
            {
                return model;
            }
        }

        return null;
    }

    private static boolean canBeUsedImplicitly(ParametrizedModel parametrizedModel)
    {
        for (ParameterModel parameterModel : parametrizedModel.getParameterModels())
        {
            if (parameterModel.isRequired() && parameterModel.getDefaultValue() == null)
            {
                return false;
            }
        }

        return true;
    }
}
