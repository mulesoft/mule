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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpToMuleMessage
{
    private static Logger logger = LoggerFactory.getLogger(HttpToMuleMessage.class);

    public static DataType buildContentTypeDataType(final String contentTypeValue)
    {
        DataTypeBuilder dataTypeBuilder = DataType.builder();

        if (contentTypeValue != null)
        {
            try
            {
                dataTypeBuilder.mimeType(contentTypeValue);
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
                    String encoding = defaultCharset().name();
                    logger.warn(format("%s when parsing Content-Type '%s': %s", e.getClass().getName(), contentTypeValue, e.getMessage()));
                    logger.warn(format("Using default encoding: %s", encoding));
                    dataTypeBuilder.encoding(encoding);
                }
            }
        }
        final DataType dataType = dataTypeBuilder.build();
        if (dataType.getEncoding() == null)
        {
            // TODO MULE-9958 provide a default encoding it the builder has null
            return DataType.builder(dataType).encoding(defaultCharset().name()).build();
        }
        else
        {
            return dataType;
        }
    }

}
