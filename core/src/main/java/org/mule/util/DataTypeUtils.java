/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.api.transformer.DataType;

/**
 * Provides utilities to manage {@link DataType}
 */
public class DataTypeUtils
{

    private DataTypeUtils()
    {
    }

    /**
     * Generates a HTTP content type for a given data type
     *
     * @param dataType datatype to generate the content type for. Not null
     * @return a {@link String} representing the datatype as a content type
     */
    public static String getContentType(DataType<?> dataType)
    {
        return dataType.getMimeType() + (StringUtils.isEmpty(dataType.getEncoding()) ? "" : "; charset=" + dataType.getEncoding());
    }
}
