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
 * <code>TransformerSession</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface TransformerSession
{
    public abstract void begin() throws TransformerException;

    public abstract void commit() throws TransformerException;

    public abstract void rollback() throws TransformerException;

    public abstract Object getData();

    /**
     * @return
     */
    public abstract boolean isInSession();

    /**
     * @return
     */
    public Object transform(UMOTransformer trans, Object data) throws TransformerException;
}