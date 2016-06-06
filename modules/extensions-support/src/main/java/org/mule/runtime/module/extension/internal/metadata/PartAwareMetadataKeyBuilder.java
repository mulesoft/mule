/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.utils.StringUtils.isNotEmpty;

import org.mule.runtime.api.metadata.BaseMetadataKeyBuilder;
import org.mule.runtime.api.metadata.DefaultMetadataKey;
import org.mule.runtime.api.metadata.MetadataKey;

/**
 * {@link BaseMetadataKeyBuilder} implementation which adds de capability of create {@link DefaultMetadataKey} with
 * a configured {@code partName}
 *
 * @since 4.0
 */
class PartAwareMetadataKeyBuilder extends BaseMetadataKeyBuilder<PartAwareMetadataKeyBuilder>
{

    private final String partName;

    private PartAwareMetadataKeyBuilder(String id, String partName)
    {
        super(id);
        this.partName = partName;
    }

    /**
     * Creates and returns new instance of a {@link PartAwareMetadataKeyBuilder}, to help building a new {@link MetadataKey}
     * represented by the given {@param id}
     *
     * @param id of the {@link MetadataKey} to be created
     * @return an initialized instance of {@link PartAwareMetadataKeyBuilder}
     */
    static PartAwareMetadataKeyBuilder newKey(String id, String partName)
    {
        return new PartAwareMetadataKeyBuilder(id, partName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataKey build()
    {
        String name = isNotEmpty(displayName) ? displayName : id;
        return new DefaultMetadataKey(id, name, properties, childs.stream().map(PartAwareMetadataKeyBuilder::build).collect(toSet()), partName);
    }
}
