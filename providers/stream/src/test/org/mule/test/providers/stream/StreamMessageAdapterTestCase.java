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

package org.mule.test.providers.stream;

import org.mule.providers.stream.StreamMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * <code>StreamMEssageAdapterTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class StreamMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    /* (non-Javadoc)
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessageTypeNotSupportedException
    {
        return new StreamMessageAdapter(payload.toString());
    }

    /* (non-Javadoc)
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return "TestMessage";
    }

    /* (non-Javadoc)
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getInvalidMessage()
     */
    public Object getInvalidMessage()
    {
        return null;
    }
}
