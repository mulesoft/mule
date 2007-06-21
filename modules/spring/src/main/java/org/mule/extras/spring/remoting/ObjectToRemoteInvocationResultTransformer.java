/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.remoting;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.ObjectOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.remoting.support.RemoteInvocationResult;

public class ObjectToRemoteInvocationResultTransformer extends AbstractTransformer
{

    public ObjectToRemoteInvocationResultTransformer()
    {
        super();
        setReturnClass(byte[].class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("ObjectToRemoteInvocationResult.doTransform(" + src + ")");
            }

            RemoteInvocationResult rval;

            if (src instanceof RemoteInvocationResult)
            {
                rval = (RemoteInvocationResult)src;
            }
            else
            {
                rval = new RemoteInvocationResult(src);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(rval);
            oos.close();
            return baos.toByteArray();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
