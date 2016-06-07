package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.DefaultMetadataKey;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;

/**
  * Extension of {@link MetadataKeyBuilder} which adds de capability of create {@link DefaultMetadataKey} with
  * a configured {@code partName}
  *
  * @since 4.0
  */
public class PartAwareMetadataKeyBuilder extends MetadataKeyBuilder
{

    private PartAwareMetadataKeyBuilder(String id, String partName)
    {
        super(id);
        setPartName(partName);
    }

    /**
     * Creates and returns new instance of a {@link PartAwareMetadataKeyBuilder}, to help building a new {@link MetadataKey}
     * represented by the given {@param id}
     *
     * @param id of the {@link MetadataKey} to be created
     * @return an initialized instance of {@link PartAwareMetadataKeyBuilder}
     */
    public static PartAwareMetadataKeyBuilder newKey(String id, String partName)
    {
        return new PartAwareMetadataKeyBuilder(id, partName);
    }
}
