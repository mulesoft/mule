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

/**
 * <code>SingleTransformerSession</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SingleTransformerSession implements TransformerSession
{
    private UMOTransformer transformer = null;

    private boolean inSession = false;

    private Object sourceData = null;

    private Object transData = null;

    public SingleTransformerSession(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }

    public synchronized void begin() throws TransformerException
    {
        if (inSession)
        {
            throw new TransformerException("The session is already active");
        }

        inSession = true;
        sourceData = null;
        transData = null;
    }

    public synchronized void commit() throws TransformerException
    {
        if (!inSession)
        {
            throw new TransformerException("The session is not active");
        }
        sourceData = null;
        inSession = false;

    }

    public synchronized void rollback() throws TransformerException
    {
        if (!inSession)
        {
            throw new TransformerException("The session is not active");
        }
        transData = sourceData;
        sourceData = null;
        inSession = false;
    }

    public Object getData()
    {
        return transData;
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
    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public Object transform(Object src) throws TransformerException
    {
        if (!isInSession()) begin();

        sourceData = src;
        transData = transformer.transform(src);
        return transData;
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.TransformerSession#transform(org.mule.umo.transformer.UMOTransformer, java.lang.Object)
     */
    public Object transform(UMOTransformer trans, Object data) throws TransformerException
    {
        if (trans != null && !trans.equals(transformer) && isInSession())
        {
            throw new TransformerException("Cannot transform on different transformer while in a session");
        }
        transformer = trans;
        return transform(data);
    }

}
