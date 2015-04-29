/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.transformer.DataType;
import org.mule.transformer.types.DataTypeFactory;

import javax.xml.stream.XMLStreamReader;

/**
 * Transforms an {@link javax.xml.stream.XMLStreamReader} to a byte array. Provides the same behavior of the
 * {@link org.mule.module.xml.transformer.XmlToDomDocument} transformer, but narrowing down the source types to only
 * support XMLStreamReader.
 */
public class XMLStreamReaderToByteArray extends XmlToDomDocument
{

    public XMLStreamReaderToByteArray()
    {
        for (DataType type : getSourceDataTypes())
        {
            if (!type.getType().equals(XMLStreamReader.class))
            {
                unregisterSourceType(type);
            }
        }
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

}
