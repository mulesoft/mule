/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.editors;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Collection;
import java.util.Iterator;

public class TransformerChain implements UMOTransformer
{

    private UMOTransformer delegate;

    public void setTransformers(Collection transformers)
    {
        Iterator iterator = transformers.iterator();
        while (iterator.hasNext())
        {
            addTransformer((UMOTransformer) iterator.next());
        }
    }

    // this must *not* be called setTransformer or it will "hide" the plural method
    // from the bean assembler and we'll only get the last transformer set
    public void addTransformer(UMOTransformer transformer)
    {
        if (null == delegate)
        {
            delegate = transformer;
        }
        else
        {
            UMOTransformer last = delegate;
            while (null != last.getNextTransformer())
            {
                last = last.getNextTransformer();
            }
            last.setNextTransformer(transformer);
        }
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        return delegate.isSourceTypeSupported(aClass);
    }

    public boolean isAcceptNull()
    {
        return delegate.isAcceptNull();
    }

    public Object transform(Object src) throws TransformerException
    {
        return delegate.transform(src);
    }

    public void setReturnClass(Class theClass)
    {
        delegate.setReturnClass(theClass);
    }

    public Class getReturnClass()
    {
        return delegate.getReturnClass();
    }

    public UMOTransformer getNextTransformer()
    {
        return delegate.getNextTransformer();
    }

    public void setNextTransformer(UMOTransformer nextTransformer)
    {
        delegate.setNextTransformer(nextTransformer);
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return delegate.getEndpoint();
    }

    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        delegate.setEndpoint(endpoint);
    }

    public void setName(String newName)
    {
        delegate.setName(newName);
    }

    public String getName()
    {
        return delegate.getName();
    }

    public void initialise() throws InitialisationException
    {
        delegate.initialise();
    }

}
