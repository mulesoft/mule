/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.util;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpToMuleMessage
{
    private static Logger logger = LoggerFactory.getLogger(HttpToMuleMessage.class);

    /**
     * 
     * @param contentTypeValue
     * @param defaultCharset the encoding to use if the given {@code contentTypeValue} doesn't have
     *            a {@code charset} parameter.
     * @return
     */
    public static DataType buildContentTypeDataType(final String contentTypeValue, Charset defaultCharset)
    {
        DataTypeBuilder dataTypeBuilder = DataType.builder();

        if (contentTypeValue != null)
        {
            try
            {
                dataTypeBuilder.mediaType(contentTypeValue);
            }
            catch (IllegalArgumentException e)
            {
                // need to support invalid Content-Types
                if (parseBoolean(System.getProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType")))
                {
                    throw e;
                }
                else
                {
                    logger.warn(format("%s when parsing Content-Type '%s': %s", e.getClass().getName(), contentTypeValue, e.getMessage()));
                    logger.warn(format("Using default encoding: %s", defaultCharset().name()));
                    dataTypeBuilder.charset(defaultCharset());
                }
            }
        }
        final DataType dataType = dataTypeBuilder.build();
        if (!dataType.getMediaType().getCharset().isPresent())
        {
            return DataType.builder(dataType).charset(defaultCharset).build();
        }
        else
        {
            return dataType;
        }
    }

}
