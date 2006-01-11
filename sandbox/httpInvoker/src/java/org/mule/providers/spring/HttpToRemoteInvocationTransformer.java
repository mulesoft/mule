package org.mule.providers.spring;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.springframework.remoting.support.RemoteInvocation;

public class HttpToRemoteInvocationTransformer extends AbstractTransformer
{
    protected transient Log logger = LogFactory.getLog(getClass());
    private static final long serialVersionUID = -7067819657247418549L;

    public HttpToRemoteInvocationTransformer()
    {
        super();
        this.registerSourceType(byte[].class);
        this.setReturnClass(RemoteInvocation.class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        if (logger.isDebugEnabled()) logger.debug("HttpToRemoteInvocation.doTransform(" + src + ")");

        try
        {
            byte[] data = (byte[]) src;
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            RemoteInvocation ri = (RemoteInvocation) o;
            if (logger.isDebugEnabled())
            {
                logger.debug("request to execute " + ri.getMethodName());
                for (int i=0;i < ri.getArguments().length;i++)
                {
                    Object a = ri.getArguments()[i];
                    logger.debug("with argument (" + a.toString() + ")");
                }
            }
            return ri;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
