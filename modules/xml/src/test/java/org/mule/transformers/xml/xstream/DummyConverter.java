/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
