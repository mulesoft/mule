/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_PREFIX;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_SUFFIX;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Base class for {@link ObjectFactory} implementation which create
 * extension components.
 * <p>
 * Contains behavior to obtain and manage components parameters.
 *
 * @param <T> the generic type of the instances to be built
 * @since 4.0
 */
public abstract class AbstractExtensionObjectFactory<T> implements ObjectFactory<T>
{

    private Map<String, Object> parameters = new HashMap<>();

    /**
     * @return the components parameters as {@link Map} which key is the
     * parameter name and the value is the actual thingy
     */
    protected Map<String, Object> getParameters()
    {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters)
    {
        this.parameters = normalize(parameters);
    }

    /**
     * Constructs a {@link ResolverSet} from the output of {@link #getParameters()},
     * using {@link #toValueResolver(Object)} to process the values.
     *
     * @return a {@link ResolverSet}
     * @throws {@link ConfigurationException} if the exclusiveness condition between parameters is not honored
     */
    protected ResolverSet getParametersAsResolverSet(EnrichableModel model, String component, String componentName) throws ConfigurationException
    {
        ResolverSet resolverSet = new ResolverSet();
        getParameters().forEach((key, value) -> resolverSet.add(key, toValueResolver(value)));
        checkParameterGroupExclusivenessForModel(model, component, componentName, getParameters().keySet());
        return resolverSet;
    }

    /**
     * Wraps the {@code value} into a {@link ValueResolver} of the proper type.
     * For example, {@link Collection} and {@link Map} instances are exposed as
     * {@link CollectionValueResolver} and {@link MapValueResolver} respectively.
     * <p>
     * If {@code value} is already a {@link ValueResolver} then it's returned as is.
     * <p>
     * Other values (including {@code null}) are wrapped in a {@link StaticValueResolver}.
     *
     * @param value the value to expose
     * @return a {@link ValueResolver}
     */
    protected ValueResolver<?> toValueResolver(Object value)
    {
        ValueResolver<?> resolver;
        if (value instanceof ValueResolver)
        {
            resolver = (ValueResolver<?>) value;
        }
        else if (value instanceof Collection)
        {
            resolver = CollectionValueResolver.of((Class<? extends Collection>) value.getClass(), (List) ((Collection) value).stream().map(this::toValueResolver).collect(new ImmutableListCollector()));
        }
        else if (value instanceof Map)
        {
            Map<Object, Object> map = (Map<Object, Object>) value;
            Map<ValueResolver<Object>, ValueResolver<Object>> normalizedMap = new LinkedHashMap<>(map.size());
            map.forEach((key, entryValue) -> normalizedMap.put((ValueResolver<Object>) toValueResolver(key), (ValueResolver<Object>) toValueResolver(entryValue)));
            resolver = MapValueResolver.of(map.getClass(), copyOf(normalizedMap.keySet()), copyOf(normalizedMap.values()));
        }
        else
        {
            resolver = new StaticValueResolver<>(value);
        }
        return resolver;
    }

    private Map<String, Object> normalize(Map<String, Object> parameters)
    {
        Map<String, Object> normalized = new HashMap<>();

        parameters.forEach((key, value) -> {
            String normalizedKey = key;

            if (isChildKey(key))
            {
                normalizedKey = unwrapChildKey(key);
                normalized.put(normalizedKey, value);
            }
            else
            {
                if (!normalized.containsKey(normalizedKey))
                {
                    normalized.put(normalizedKey, value);
                }
            }
        });

        return normalized;
    }

    private boolean isChildKey(String key)
    {
        return key.startsWith(CHILD_ELEMENT_KEY_PREFIX) && key.endsWith(CHILD_ELEMENT_KEY_SUFFIX);
    }

    private String unwrapChildKey(String key)
    {
        return key.replaceAll(CHILD_ELEMENT_KEY_PREFIX, "").replaceAll(CHILD_ELEMENT_KEY_SUFFIX, "");
    }

    private void checkParameterGroupExclusivenessForModel(EnrichableModel model, String component, String componentName, Set<String> resolverKeys) throws ConfigurationException
    {
        Optional<ParameterGroupModelProperty> parameterGroupModelProperty = model.getModelProperty(ParameterGroupModelProperty.class);
        if (parameterGroupModelProperty.isPresent() && parameterGroupModelProperty.get().hasExclusion())
        {
            checkParameterGroupExclusiveness(parameterGroupModelProperty.get(), component, componentName, resolverKeys);
        }
    }

    private void checkParameterGroupExclusiveness(ParameterGroupModelProperty parameterGroupModelProperty, String component, String componentName, Set<String> resolverKeys) throws ConfigurationException
    {
        for (ParameterGroup group : parameterGroupModelProperty.getGroups())
        {
            Set<String> parametersFromGroup = group.getParameters().stream()
                    .filter(p -> p.getAnnotation(org.mule.runtime.extension.api.annotation.param.Optional.class) != null)
                    .map(p -> p.getName())
                    .filter(name -> resolverKeys.contains(name))
                    .collect(toSet());

            if (parametersFromGroup.size() > 1)
            {
                StringJoiner joiner = new StringJoiner(", ");
                parametersFromGroup.stream().forEach(p -> joiner.add(p));
                throw new ConfigurationException(createStaticMessage(format("In %s '%s', parameter group '%s' has been set with the following parameters [%s] but only one should be set at any time due to its exclusive relation", component, componentName, group.getType().getName(), joiner.toString())));
            }

            if (group.oneRequired() && parametersFromGroup.isEmpty())
            {
                throw new ConfigurationException((createStaticMessage(format("Parameter group '%s' requires that one of its optional parameters should be set but all of them are missing", group.getType().getName()))));
            }
        }
    }
}
