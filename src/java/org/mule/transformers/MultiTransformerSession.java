/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.transformers;

import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.LinkedList;

/**
 * <code>SingleTransformerSession</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MultiTransformerSession implements TransformerSession
{
    public static final int MAX_STACK_SIZE = 20;

    private boolean inSession = false;

    private LinkedList transformStack = new LinkedList();

    /* either the transformed object or the source depending on the state of the session
     * if thre session is rolled back this will be the source data otherwise the
     * currently transformed data
     */
    private Object data = null;

    public MultiTransformerSession()
    {
    }

    public synchronized void begin() throws TransformerException
    {
        if (inSession)
        {
            throw new TransformerException("The session is already active");
        }

        inSession = true;
        data = null;
    }

    public synchronized void commit() throws TransformerException
    {
        if (!inSession)
        {
            throw new TransformerException("The session is not active");
        }

        inSession = false;
        transformStack = new LinkedList();

    }

    public synchronized void rollback() throws TransformerException
    {
        if (!inSession)
        {
            throw new TransformerException("The session is not active");
        }
        //a transform may have not have happened yet
        if (transformStack.size() != 0)
        {
            data = transformStack.getFirst();
        }
        inSession = false;
        transformStack = new LinkedList();
    }

    public Object getData()
    {
        return data;
    }

    /**
     * @return
     */
    public boolean isInSession()
    {
        return inSession;
    }

    /**
     * @return
     */
    public Object transform(UMOTransformer transformer, Object src) throws TransformerException
    {
        if (!isInSession()) begin();

        if (transformStack.size() == 0) pushToStack(src);
        data = transformer.transform(src);
        pushToStack(data);
        return data;
    }

    void pushToStack(Object data)
    {
        if (transformStack.size() >= MAX_STACK_SIZE)
        {
            throw new IllegalStateException("The trnasform stack in the MultiTransform session has exceeded it's maximum size.");
        }
        transformStack.add(data);
        this.data = data;
    }

    public Object getFromStack(int index)
    {
        if (index > transformStack.size() || index < 0)
        {
            return null;
        }
        return transformStack.get(index);
    }

    public int getStackSize()
    {
        return transformStack.size();
    }

}
