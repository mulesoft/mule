/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import org.mule.metadata.api.model.MetadataType;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable container for type mapping, storing the relation of a given type and its declared subtypes
 *
 * @since 4.0
 */
public class SubTypesMappingContainer
{

    private final Map<MetadataType, List<MetadataType>> subTypesMapping;

    public SubTypesMappingContainer(Map<MetadataType, List<MetadataType>> subTypesMapping)
    {
        this.subTypesMapping = subTypesMapping;
    }

    /**
     * Returns a {@link List} with all the declared {@link MetadataType} subtypes
     * for the indicated {@link MetadataType} {@code type}
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
