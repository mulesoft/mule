/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * A unique transformer instance than indicates an error (typically that the transformer
 * in question has not been initialised).
 */
public class VoidTransformer implements UMOTransformer
{

    private static class SingletonHolder
    {
        private static final VoidTransformer instance = new VoidTransformer();
    }

    public static VoidTransformer getInstance()
    {
        return SingletonHolder.instance;
    }

    private VoidTransformer() {
        // no-op
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        throw new IllegalStateException("Void transformer");
    }

    public boolean isAcceptNull()
    {
        throw new IllegalStateException("Void transformer");
    }

    public Object transform(Object src) throws TransformerException
    {
        throw new IllegalStateException("Void transformer");
    }

    public void setReturnClass(Class theClass)
    {
        throw new IllegalStateException("Void transformer");
    }

    public Class getReturnClass()
    {
        throw new IllegalStateException("Void transformer");
    }

    public UMOTransformer getNextTransformer()
    {
        throw new IllegalStateException("Void transformer");
    }

    public void setNextTransformer(UMOTransformer nextTransformer)
    {
        throw new IllegalStateException("Void transformer");
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        throw new IllegalStateException("Void transformer");
    }

    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        throw new IllegalStateException("Void transformer");
    }

    public void setName(String newName)
    {
        throw new IllegalStateException("Void transformer");
    }

    public String getName()
    {
        throw new IllegalStateException("Void transformer");
    }

    public void initialise() throws InitialisationException
    {
        throw new IllegalStateException("Void transformer");
    }
}
