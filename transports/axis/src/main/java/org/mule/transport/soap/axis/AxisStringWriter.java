/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
