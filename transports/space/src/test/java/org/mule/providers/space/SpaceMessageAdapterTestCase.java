/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.space;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @version $Revision$
 */
public class SpaceMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws MessagingException
    {
        return new SpaceMessageAdapter("hello");
    }

    public Object getInvalidMessage()
    {
        return null;
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new SpaceMessageAdapter(payload);
    }
}
