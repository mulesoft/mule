/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model;

import org.mule.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.ResolvableType;

/**
 *  Immutable container for type mapping, storing the relation of a given type and its declared subtypes
 *
 *  @since 4.0
 */
public class SubTypesMapper
{

    private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    private final Map<MetadataType, List<MetadataType>> subTypesMapping = new HashMap<>();

    public SubTypesMapper(Map<Class<?>, List<Class<?>>> mapping)
    {
        mapping.keySet().stream()
                .forEach(baseType -> subTypesMapping.put(loadType(baseType), mapping.get(baseType).stream().map(s -> loadType(s))
                        .collect(Collectors.toList())));
    }

    private MetadataType loadType(Class<?> baseType)
    {
        return typeLoader.load(ResolvableType.forClass(baseType).getType());
    }

    /**
     * Returns a {@link List} with all the declared subtypes for the indicated {@link MetadataType}
     *
     * @param type the {@link MetadataType} for which to retrieve its declared subTypes
     * @return a {@link List} with all the declared subtypes for the indicated {@link MetadataType}
     */
    public List<MetadataType> getSubTypes(MetadataType type)
    {
        List<MetadataType> subTypes = subTypesMapping.get(type);
        return subTypes != null ? ImmutableList.copyOf(subTypes) : Collections.emptyList();
    }

}
