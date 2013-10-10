/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;

import java.util.List;

public class DummyMuleContextAware implements MuleContextAware, Transformer
{

    public void setMuleContext(MuleContext context)
    {
        // empty
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        return false;
    }

    public List<Class<?>> getSourceTypes()
    {
        return null;
    }

    public boolean isAcceptNull()
    {
        return false;
    }

    public boolean isIgnoreBadInput()
    {
        return false;
    }

    public Object transform(Object src, String encoding) throws TransformerException
    {
        return null;
    }

    public Object transform(Object src) throws TransformerException
    {
        return null;
    }

    public void setReturnClass(Class clazz)
    {
        //no-op
    }

    public Class getReturnClass()
    {
        return null;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return null;
    }

    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        // empty
    }

    public void initialise() throws InitialisationException
    {
        // empty
    }

    public void dispose()
    {
        // empty
    }

    public void setName(String name)
    {
        // empty
    }

    public String getName()
    {
        return null;
    }

    public void setReturnDataType(DataType type)
    {
        //empty
    }

    public DataType getReturnDataType()
    {
        return null;
    }

    public boolean isSourceDataTypeSupported(DataType dataType)
    {
        return false;
    }

    public List<DataType<?>> getSourceDataTypes()
    {
        return null;
    }

    public String getMimeType()
    {
        return null;
    }

    public String getEncoding()
    {
        return null;  
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return null;
    }
}
