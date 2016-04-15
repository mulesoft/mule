/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.metadata;

import org.mule.common.DefaultResult;
import org.mule.common.Result;
import org.mule.common.metadata.DefaultListMetaDataModel;
import org.mule.common.metadata.DefaultMetaData;
import org.mule.common.metadata.DefaultSimpleMetaDataModel;
import org.mule.common.metadata.ListMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataModel;
import org.mule.common.metadata.datatype.DataType;

/**
 * Provides metadata for bulk queries
 */
public class BulkExecuteMetadataProvider implements QueryMetadataProvider
{

    @Override
    public Result<MetaData> getInputMetaData()
    {
        return null;
    }

    @Override
    public Result<MetaData> getOutputMetaData(MetaData metaData)
    {
        DefaultMetaData defaultMetaData;


        MetaDataModel recordModel = new DefaultSimpleMetaDataModel(DataType.DOUBLE);
        ListMetaDataModel listModel = new DefaultListMetaDataModel(recordModel, true);
        defaultMetaData = new DefaultMetaData(listModel);

        return new DefaultResult<MetaData>(defaultMetaData);
    }
}
