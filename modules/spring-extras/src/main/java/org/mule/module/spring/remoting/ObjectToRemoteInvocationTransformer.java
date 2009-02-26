/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.remoting;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.springframework.remoting.support.RemoteInvocation;

/**
 * Transforms a byte[] into an ObjectInputStream and then into a Spring RemoteInvocation instance.
 */

public class ObjectToRemoteInvocationTransformer extends AbstractTransformer
{

    public ObjectToRemoteInvocationTransformer()
    {
        super();
        this.registerSourceType(RemoteInvocation.class);
        this.registerSourceType(byte[].class);
        this.registerSourceType(InputStream.class);
        this.setReturnClass(RemoteInvocation.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof RemoteInvocation)
        {
            return src;
        }

        Object o = null;

        if (src instanceof InputStream)
        {
            try
            {
                o = new ObjectInputStream((InputStream) src).readObject();
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        }
        else
        {
            byte[] data = (byte[]) src;
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try
            {
                ObjectInputStream ois = new ObjectInputStream(bais);
                o = ois.readObject();
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        }

        RemoteInvocation ri = (RemoteInvocation) o;
        if (logger.isDebugEnabled())
        {
            logger.debug("request to execute " + ri.getMethodName());
            for (int i = 0; i < ri.getArguments().length; i++)
            {
                Object a = ri.getArguments()[i];
                logger.debug("with argument (" + a.toString() + ")");
            }
        }
        return ri;

    }

}
