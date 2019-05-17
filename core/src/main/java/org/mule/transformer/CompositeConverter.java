/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;

import java.util.LinkedList;
import java.util.List;

/**
 * Composes many converters to behave as a single one.
 * <p/>
 * When {@link #transform(Object)} is called each converter in the same order
 * they are included in the composition.
 * The output of a given converter is the input of the next composed converter.
 */
public class CompositeConverter implements Converter
{

    private String name;

    private LinkedList<Converter> chain;

    /**
     * Create a new conversion chain using the specified converters
     *
     * @param converters List of converters using to build the chain
     */
    public CompositeConverter(Converter... converters)
    {
        if (converters.length == 0)
        {
            throw new IllegalArgumentException("There must be at least one converter");
        }

        chain = new LinkedList<Converter>();

        name = compositeConverterName(converters);
    }

    private String compositeConverterName(Converter[] converters)
    {
        StringBuilder builder = new StringBuilder();
        for (Converter converter : converters)
        {
            chain.addLast(converter);
            builder.append(converter.getName());
        }

        return builder.toString();
    }

    @Override
    public boolean isSourceTypeSupported(Class<?> aClass)
    {
        return chain.size() > 0 && chain.peekFirst().isSourceTypeSupported(aClass);
    }

    @Override
    public boolean isSourceDataTypeSupported(DataType<?> dataType)
    {
        return chain.size() > 0 && chain.peekFirst().isSourceDataTypeSupported(dataType);
    }

    @Override
    public List<Class<?>> getSourceTypes()
    {
        return chain.peekFirst().getSourceTypes();
    }

    @Override
    public List<DataType<?>> getSourceDataTypes()
    {
        return chain.peekFirst().getSourceDataTypes();
    }

    @Override
    public boolean isAcceptNull()
    {
        return chain.size() > 0 && chain.peekFirst().isAcceptNull();
    }

    @Override
    public boolean isIgnoreBadInput()
    {
        return chain.size() > 0 && chain.peekFirst().isIgnoreBadInput();
    }

    @Override
    public Object transform(Object src) throws TransformerException
    {
        return transform(src, null);
    }

    @Override
    public Object transform(Object src, String encoding) throws TransformerException
    {
        Object current = src;
        String currentEncoding = encoding;
        for (Converter converter : chain)
        {
            if (currentEncoding != null)
            {
                current = converter.transform(current, currentEncoding);
            }
            else
            {
                current = converter.transform(current);
            }
            currentEncoding = converter.getEncoding();
        }

        return current;
    }

    @Override
    public void setReturnClass(Class<?> theClass)
    {
        if (chain.size() > 0)
        {
            chain.peekLast().setReturnClass(theClass);
            return;
        }

        throw new IllegalStateException("Cannot set return class on an empty converter chain");
    }

    @Override
    public Class<?> getReturnClass()
    {
        return chain.peekLast().getReturnClass();
    }

    @Override
    public void setReturnDataType(DataType<?> type)
    {
        chain.peekLast().setReturnDataType(type);
    }

    @Override
    public DataType<?> getReturnDataType()
    {
        return chain.peekLast().getReturnDataType();
    }

    @Override
    public String getMimeType()
    {
        return chain.peekLast().getMimeType();
    }

    @Override
    public String getEncoding()
    {
        return chain.peekLast().getEncoding();
    }

    @Override
    public ImmutableEndpoint getEndpoint()
    {
        return chain.peekFirst().getEndpoint();
    }

    @Override
    public void dispose()
    {
        for (Converter converter : chain)
        {
            converter.dispose();
        }
    }

    @Override
    public void setEndpoint(ImmutableEndpoint ep)
    {
        for (Converter converter : chain)
        {
            converter.setEndpoint(ep);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        for (Converter converter : chain)
        {
            converter.initialise();
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && event.getMessage() != null)
        {
            try
            {
                event.getMessage().applyTransformers(event, this);
            }
            catch (Exception e)
            {
                throw new TransformerMessagingException(event, this, e);
            }
        }

        return event;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        for (Converter converter : chain)
        {
            converter.setMuleContext(context);
        }
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException("Cannot change composite converter name");
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getPriorityWeighting()
    {
        int priorityWeighting = 0;
        for (Converter converter : chain)
        {
            priorityWeighting += converter.getPriorityWeighting();
        }

        return priorityWeighting;
    }

    @Override
    public void setPriorityWeighting(int weighting)
    {
    }

    public LinkedList<Converter> getConverters()
    {
        return new LinkedList<Converter>(chain);
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "[name: " + getName() + "; chain: " + getConverters().toString() + "]";
    }
}
