package org.mule.providers.spring;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.springframework.remoting.support.RemoteInvocationResult;

public class ObjectToRemoteInvocationResultTransformer extends AbstractTransformer
{
    protected transient Log logger = LogFactory.getLog(getClass());
    private static final long serialVersionUID = -7067819657247418549L;

    public ObjectToRemoteInvocationResultTransformer()
    {
        super();
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled()) logger.debug("ObjectToRemoteInvocationResult.doTransform(" + src + ")");
            
            RemoteInvocationResult rval;

            if (src instanceof RemoteInvocationResult)
            {
                rval = (RemoteInvocationResult) src;
            }
            else
            {
                rval = new RemoteInvocationResult(src);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(rval);
            oos.close();
            byte[] results = baos.toByteArray();
            return results;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
