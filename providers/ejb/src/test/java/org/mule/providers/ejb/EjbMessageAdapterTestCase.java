/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.ejb;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */
public class EjbMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new EjbMessageAdapter(payload);
    }

    public Object getInvalidMessage()
    {
        return null;
    }
}
