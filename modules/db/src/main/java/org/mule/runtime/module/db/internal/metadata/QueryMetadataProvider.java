/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.metadata;

import org.mule.common.Result;
import org.mule.common.metadata.MetaData;

/**
 * Provides metadata for a given type of query
 */
public interface QueryMetadataProvider
{
    /**
     * Provides input metadata
     *
     * @return input metadata for a given type of query. Can be null
     */
    Result<MetaData> getInputMetaData();

    /**
     * Provides output metadata
     * @param metaData metadata propagated from the input
     * @return input metadata for a given type of query. Can be null
     */
    Result<MetaData> getOutputMetaData(MetaData metaData);
}
