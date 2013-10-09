/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.api.transformer.DataType;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;

import javax.activation.MimeTypeParseException;

import org.ibeans.api.channel.MimeType;

/**
 * Both Mule and iBeans define a DataType model for associating Java types with other info such as mime type and encoding
 * This classes provides a couple of functions to convert between the two models
 */
public class DataTypeConverter
{
    public static org.ibeans.api.DataType convertMuleToIBeans(DataType muleDT) throws MimeTypeParseException
    {
    //Both Mule and iBeans have DataType implementations, need to wrap the Mule DataType to work with iBeans
        if(muleDT instanceof CollectionDataType)
        {
            CollectionDataType dt = (CollectionDataType)muleDT;
            return org.ibeans.impl.support.datatype.DataTypeFactory.create(dt.getType(), dt.getItemType(), new MimeType(dt.getMimeType()));
        }
        else
        {
            return org.ibeans.impl.support.datatype.DataTypeFactory.create(muleDT.getType(), new MimeType(muleDT.getMimeType()));
        }

    }

    public static DataType convertIBeansToMule(org.ibeans.api.DataType ibeansDT) throws MimeTypeParseException
    {
    //Both Mule and iBeans have DataType implementations, need to wrap the Mule DataType to work with iBeans
        if(ibeansDT instanceof org.ibeans.impl.support.datatype.CollectionDataType)
        {
            org.ibeans.impl.support.datatype.CollectionDataType dt = (org.ibeans.impl.support.datatype.CollectionDataType)ibeansDT;
            return DataTypeFactory.create(dt.getType(), dt.getItemType(), dt.getMimeType());
        }
        else
        {
            return DataTypeFactory.create(ibeansDT.getType(), ibeansDT.getMimeType());
        }

    }
}
