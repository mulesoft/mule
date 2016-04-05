/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;


import org.mule.extension.api.introspection.ModelProperty;
import org.mule.metadata.api.model.MetadataType;
import org.mule.module.extension.internal.model.SubTypesMapper;

import java.util.List;
import java.util.Map;

/**
 * An immutable model property which specifies the relations of a given
 * {@link MetadataType} and its declared subTypes, which are concrete implementations
 * of the base {@link Class}
 *
 * @since 4.0
 */
public final class SubTypesModelProperty implements ModelProperty
{

    private final SubTypesMapper typesMap;

    /**
     * Creates a new instance containing all the baseType-subTypes declarations
     * based on the {@link Class} references of each type
     *
     * @param mapping concrete implementations of the {@code baseType}
     */
    public SubTypesModelProperty(Map<Class<?>, List<Class<?>>> mapping)
    {
        this.typesMap = new SubTypesMapper(mapping);
    }

    /**
     * @return the {@link SubTypesMapper} container which holds the relationships of types and subTypes
     */
    public SubTypesMapper getSubTypesMapping()
    {
        return typesMap;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code subTypesMapping}
     */
    @Override
    public String getName()
    {
        return "subTypesMapping";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true}
     */
    @Override
    public boolean isExternalizable()
    {
        return true;
    }
}
