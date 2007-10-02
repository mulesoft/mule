/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stdio;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;

public class StdioMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessageTypeNotSupportedException
    {
        return new StdioMessageAdapter(payload);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return "TestMessage";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getInvalidMessage()
     */
    public Object getInvalidMessage()
    {
        return null;
    }
}
