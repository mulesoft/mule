/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.impl.MuleMessage;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * <code>VMMessageAdapterTestCase</code> TODO (document class)
 */
public class VMMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessageTypeNotSupportedException
    {
        if (payload instanceof UMOMessage)
        {
            return new VMMessageAdapter((UMOMessage)payload);
        }
        else
        {
            throw new MessageTypeNotSupportedException(payload, VMMessageAdapter.class);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage() throws UMOException
    {
        return new MuleMessage("Valid Message");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getInvalidMessage()
     */
    public Object getInvalidMessage()
    {
        return "Invalid message";
    }

}
