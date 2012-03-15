/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;

import java.util.LinkedList;
import java.util.List;

/**
 * Composes many transformers to behave as a single one.
 * <p/>
 * When {@link #transform(Object)} is called each transformer in the same order they are included in the composition.
 * The output of a given transformer is the input of the next composed transformer.
 */
public class CompositeConverter implements Transformer, Converter
{

    private String name;

    public LinkedList<Transformer> getChain()
    {
        //GUARDA: esto lo meti para testear, tendria que implementar equals mejor y hashcode tambien
        return chain;
    }

    protected LinkedList<Transformer> chain;

    /**
     * Create a new transformation chain using the specified transformers
     *
     * @param transformers List of transformers using to build the chain
     */
    public CompositeConverter(Transformer... transformers)
    {
        if (transformers.length == 0)
        {
            throw new IllegalArgumentException("There must be at least one transformer");
        }

        chain = new LinkedList<Transformer>();

        for (Transformer transformer : transformers)
        {
            if (!(transformer instanceof Converter))
            {
                throw new IllegalArgumentException("Transformer must implement Converter interface");
            }
            chain.addLast(transformer);
        }
    }

    /**
     * Determines if a particular source class can be handled by this transformer
     *
     * @param aClass The class to check for compatibility
     * @return true if the transformer supports this type of class or false
     *         otherwise
     * @deprecated use {@link #isSourceDataTypeSupported(org.mule.api.transformer.DataType)} instead
     */
    @Override
    public boolean isSourceTypeSupported(Class<?> aClass)
    {
        return chain.size() > 0 && chain.peekFirst().isSourceTypeSupported(aClass);
    }

    /**
     * Determines if a particular source class can be handled by this transformer
     *
     * @param dataType The DataType to check for compatibility
     * @return true if the transformer supports this type of class or false
     *         otherwise
     * @since 3.0.0
     */
    @Override
    public boolean isSourceDataTypeSupported(DataType<?> dataType)
    {
        return chain.size() > 0 && chain.peekFirst().isSourceDataTypeSupported(dataType);
    }

    /**
     * Returns an unmodifiable list of Source types registered on this transformer
     *
     * @return an unmodifiable list of Source types registered on this transformer
     * @deprecated use {@link #getSourceDataTypes()} instead
     */
    @Override
    public List<Class<?>> getSourceTypes()
    {
        return chain.peekFirst().getSourceTypes();
    }

    /**
     * Returns an unmodifiable list of Source types registered on this transformer
     *
     * @return an unmodifiable list of Source types registered on this transformer
     * @since 3.0.0
     */
    @Override
    public List<DataType<?>> getSourceDataTypes()
    {
        return chain.peekFirst().getSourceDataTypes();
    }

    /**
     * Does this transformer allow null input?
     *
     * @return true if this transformer can accept null input
     */
    @Override
    public boolean isAcceptNull()
    {
        return chain.size() > 0 && chain.peekFirst().isAcceptNull();
    }

    /**
     * By default, Mule will throw an exception if a transformer is invoked with a source object that is not compatible
     * with the transformer. Since transformers are often chained, it is useful to be able to ignore a transformer in the
     * chain and move to the next one.
     *
     * @return true if the transformer can be ignored if the current source type is not supported, false if an exception
     *         should be throw due to an incompatible source type being passed in.
     */
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
        for (Transformer transformer : chain)
        {
            if (currentEncoding != null)
            {
                current = transformer.transform(current, currentEncoding);
            }
            else
            {
                current = transformer.transform(current);
            }
            currentEncoding = transformer.getEncoding();
        }

        return current;
    }

    /**
     * Sets the expected return type for the transformed data. If the transformed
     * data is not of this class type a <code>TransformerException</code> will be
     * thrown.
     *
     * @param theClass the expected return type class
     * @deprecated use {@link #setReturnDataType(DataType)} instead
     */
    @Override
    public void setReturnClass(Class<?> theClass)
    {
        if (chain.size() > 0)
        {
            chain.peekLast().setReturnClass(theClass);
            return;
        }

        throw new IllegalStateException("Cannot set return class on an empty transformer chain");
    }

    /**
     * Specifies the Java type of the result after this transformer has been executed. Mule will use this to validate
     * the return type but also allow users to perform automatic transformations based on the source type of the object
     * to transform and this return type.
     *
     * @return the excepted return type from this transformer
     * @deprecated use {@link #getReturnDataType()} instead.
     */
    @Override
    public Class<?> getReturnClass()
    {
        return chain.peekLast().getReturnClass();
    }

    /**
     * Sets the expected return type for the transformed data. If the transformed
     * data is not of this class type a <code>TransformerException</code> will be
     * thrown.
     * <p/>
     * This method supersedes {@link #getReturnClass()} because it allows Generics information to be associated with the
     * return type of the transformer
     *
     * @param type the expected return type for this transformer
     * @since 3.0.0
     */
    @Override
    public void setReturnDataType(DataType<?> type)
    {
        chain.peekLast().setReturnDataType(type);
    }

    /**
     * Specifies the return type of the result after this transformer has been executed. Mule will use this to validate
     * the return type but also allow users to perform automatic transformations based on the source type of the object
     * to transform and this return type.
     * <p/>
     * This method supersedes {@link #getReturnClass()} because it allows Generics information to be associated with the
     * return type of the transformer
     *
     * @return the excepted return type for this transformer
     * @since 3.0.0
     */
    @Override
    public DataType<?> getReturnDataType()
    {
        return chain.peekLast().getReturnDataType();
    }

    /**
     * Return the mime type returned by the transformer (if any).
     */
    @Override
    public String getMimeType()
    {
        return chain.peekLast().getMimeType();
    }

    /**
     * Return the encoding returned by the transformer (if any).
     */
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
        for (Transformer transformer : chain)
        {
            transformer.dispose();
        }
    }

    @Override
    public void setEndpoint(ImmutableEndpoint ep)
    {
        for (Transformer transformer : chain)
        {
            transformer.setEndpoint(ep);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        for (Transformer transformer : chain)
        {
            transformer.initialise();
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
        for (Transformer transformer : chain)
        {
            transformer.setMuleContext(context);
        }
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public int getPriorityWeighting()
    {
        int priorityWeighting = 0;
        for (Transformer transformer : chain)
        {
            if (transformer instanceof Converter)
            {
                priorityWeighting += ((Converter) transformer).getPriorityWeighting();
            }
        }

        return priorityWeighting;
    }

    @Override
    public void setPriorityWeighting(int weighting)
    {
    }
}
