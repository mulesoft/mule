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
        this.registerSourceType(DataTypeFactory.create(RemoteInvocation.class));
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.setReturnDataType(DataTypeFactory.create(RemoteInvocation.class));
    }

    @Override
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
                Object currentArgument = ri.getArguments()[i];
                
                StringBuilder buf = new StringBuilder(64);
                buf.append("with argument (");
                buf.append(currentArgument == null ? "<null>" : currentArgument.toString());
                buf.append(")");
                
                logger.debug(buf);
            }
        }
        return ri;
    }

}
