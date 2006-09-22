/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.rmi;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */
public class RmiMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new RmiMessageAdapter(payload);
    }

    public Object getInvalidMessage()
    {
        return null;
    }
}
