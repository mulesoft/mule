/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Used for configuration testing
 */
public class DummyConverter implements Converter
{
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        //do nothing
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        return null;
    }

    public boolean canConvert(Class type)
    {
        return false;
    }
}
