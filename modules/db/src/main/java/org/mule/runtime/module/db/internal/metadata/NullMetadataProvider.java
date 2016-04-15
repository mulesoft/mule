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
 * Null implementation of {@link QueryMetadataProvider}
 */
public class NullMetadataProvider implements QueryMetadataProvider
{

    @Override
    public Result<MetaData> getInputMetaData()
    {
        return null;
    }

    @Override
    public Result<MetaData> getOutputMetaData(MetaData metaData)
    {
        return null;
    }
}
