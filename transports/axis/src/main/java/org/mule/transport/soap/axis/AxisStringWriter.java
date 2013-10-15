/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>AxisStringWriter</code> wraps a java.io.StringWriter and allows meta information to be 
 * associated with it.
 */
public class AxisStringWriter
{
    private final StringWriter writer;
    private final Map<String, Object> properties;

    public AxisStringWriter()
    {
        writer = new StringWriter(4096);
        properties = new HashMap<String, Object>();
    }

    public void write(String string)
    {
        writer.write(string);
    }

    public void write(String string, int offset, int len)
    {
        writer.write(string, offset, len);
    }

    public Writer getWriter()
    {
        return writer;
    }

    public void flush()
    {
        writer.flush();
    }

    public void close() throws IOException
    {
        writer.close();
    }

    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }
    
    public Map<String, Object> getProperties()
    {
        return properties;
    }
}
