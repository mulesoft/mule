/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleContext;
import org.mule.transport.AbstractMuleMessageFactory;

import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;

public class CxfMuleMessageFactory extends AbstractMuleMessageFactory
{
    public CxfMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { Message.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        Message cxfMessage = (Message) transportMessage;
        
        List<Object> list = CastUtils.cast(cxfMessage.getContent(List.class));
        if (list == null)
        {
            // Seems Providers get objects stored this way
            Object object = cxfMessage.getContent(Object.class);
            if (object != null)
            {
                return object;
            }
            else
            {
                return new Object[0];
            }
        }
        
        if ((list.size() == 1) && (list.get(0) != null))
        {
            return list.get(0);
        }
        else
        {
            return list.toArray();
        }
    }
}
