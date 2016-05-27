/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Set of utility operations to handle {@link MetadataType}
 *
 * @since 4.0
 */
public class MetadataTypeUtils
{

    public static boolean isNullType(MetadataType type)
    {
        return type instanceof NullType;
    }

    public static boolean isVoid(MetadataType type)
    {
        return isNullType(type);
    }

    public static String getAliasName(MetadataType metadataType)
    {
        return IntrospectionUtils.getAliasName(JavaTypeUtils.getType(metadataType));
    }

    public static String getAliasName(MetadataType metadataType, String defaultName)
    {
        Class<?> type = JavaTypeUtils.getType(metadataType);
        return IntrospectionUtils.getAliasName(defaultName, type.getAnnotation(Alias.class));
    }

    public static MetadataType subTypesAsUnionType(MetadataType baseType, SubTypesMappingContainer subtypesContainer)
    {
        List<MetadataType> subTypes = subtypesContainer.getSubTypes(baseType);
        if (subTypes.isEmpty())
        {
            return baseType;
        }

        boolean baseIsInstantiable = isInstantiable(getType(baseType));
        if (subTypes.size() == 1 && !baseIsInstantiable)
        {
            // avoid single type union
            return subTypes.get(0);
        }

        ImmutableList.Builder<MetadataType> union = ImmutableList.<MetadataType>builder().addAll(subTypes);
        if (baseIsInstantiable)
        {
            union.add(baseType);
        }

        UnionTypeBuilder<?> unionTypeBuilder = BaseTypeBuilder.create(baseType.getMetadataFormat()).unionType();
        union.build().forEach(unionTypeBuilder::of);
        return unionTypeBuilder.build();
    }
}
