/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.util.List;

public class DummyMuleContextAware implements MuleContextAware, Transformer
{

    @Override
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

    @Override
    public boolean isAcceptNull()
    {
        return false;
    }

    @Override
    public boolean isIgnoreBadInput()
    {
        return false;
    }

    @Override
    public Object transform(Object src, String encoding) throws TransformerException
    {
        return null;
    }

    @Override
    public Object transform(Object src) throws TransformerException
    {
        return null;
    }

    public Class getReturnClass()
    {
        return null;
    }

    @Override
    public ImmutableEndpoint getEndpoint()
    {
        return null;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        // empty
    }

    @Override
    public void initialise() throws InitialisationException
    {
        // empty
    }

    @Override
    public void dispose()
    {
        // empty
    }

    @Override
    public void setName(String name)
    {
        // empty
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public void setReturnDataType(DataType type)
    {
        //empty
    }

    @Override
    public DataType getReturnDataType()
    {
        return null;
    }

    @Override
    public boolean isSourceDataTypeSupported(DataType dataType)
    {
        return false;
    }

    @Override
    public List<DataType<?>> getSourceDataTypes()
    {
        return null;
    }

    @Override
    public String getMimeType()
    {
        return null;
    }

    @Override
    public String getEncoding()
    {
        return null;  
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return null;
    }
}
