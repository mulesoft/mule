/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.remoting;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ObjectOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * Converts an Object to a Spring RemoteInvocationResult and then into a byte[].
 */

public class ObjectToRemoteInvocationResultTransformer extends AbstractTransformer
{
    public ObjectToRemoteInvocationResultTransformer()
    {
        super();
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
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
