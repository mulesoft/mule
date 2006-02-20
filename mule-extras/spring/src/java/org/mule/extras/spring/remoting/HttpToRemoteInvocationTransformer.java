/*
 * $Header:
 * /cvsroot/mule/mule/src/test/org/mule/test/mule/AbstractMuleTestCase.java,v
 * 1.7 2003/11/24 09:58:47 rossmason Exp $ $Revision$ $Date: 2003/11/24
 * 09:58:47 $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.extras.spring.remoting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.springframework.remoting.support.RemoteInvocation;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

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

    public Object doTransform(Object src, String encoding) throws TransformerException
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
            throw new TransformerException(this, e);
        }
    }
}
