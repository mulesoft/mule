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
 * A unique transformer instance than indicates the unintialised state and allows
 * atomic assignment in the presence of external updates (ie we can't use null).
 */
public class UninitialisedTransformer implements UMOTransformer
{

    private static class SingletonHolder
    {
        private static final UninitialisedTransformer instance = new UninitialisedTransformer();
    }

    public static UninitialisedTransformer getInstance()
    {
        return SingletonHolder.instance;
    }

    private UninitialisedTransformer() {
        // no-op
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        throw new NullPointerException("Null transformer");
    }

    public boolean isAcceptNull()
    {
        throw new NullPointerException("Null transformer");
    }

    public Object transform(Object src) throws TransformerException
    {
        throw new NullPointerException("Null transformer");
    }

    public void setReturnClass(Class theClass)
    {
        throw new NullPointerException("Null transformer");
    }

    public Class getReturnClass()
    {
        throw new NullPointerException("Null transformer");
    }

    public UMOTransformer getNextTransformer()
    {
        throw new NullPointerException("Null transformer");
    }

    public void setNextTransformer(UMOTransformer nextTransformer)
    {
        throw new NullPointerException("Null transformer");
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        throw new NullPointerException("Null transformer");
    }

    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        throw new NullPointerException("Null transformer");
    }

    public void setName(String newName)
    {
        throw new NullPointerException("Null transformer");
    }

    public String getName()
    {
        throw new NullPointerException("Null transformer");
    }

    public void initialise() throws InitialisationException
    {
        throw new NullPointerException("Null transformer");
    }
}
