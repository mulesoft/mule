/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>UniqueIdNotSupportedException</code> is thrown by
 * UMOMessageAdapter.getUniqueId() if the underlying message does not support or have
 * a unique identifier.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UniqueIdNotSupportedException extends MuleRuntimeException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6719055482076081111L;

    public UniqueIdNotSupportedException(UMOMessageAdapter adapter)
    {
        super(new Message(Messages.UNIQUE_ID_NOT_SUPPORTED_BY_ADAPTER_X, adapter.getClass().getName()));
    }

    public UniqueIdNotSupportedException(UMOMessageAdapter adapter, Message message)
    {
        super(chainMessage(new Message(Messages.UNIQUE_ID_NOT_SUPPORTED_BY_ADAPTER_X, adapter.getClass()
            .getName()), message));
    }

    public UniqueIdNotSupportedException(UMOMessageAdapter adapter, Throwable cause)
    {
        super(new Message(Messages.UNIQUE_ID_NOT_SUPPORTED_BY_ADAPTER_X, adapter.getClass().getName()), cause);
    }

    protected static Message chainMessage(Message m1, Message m2)
    {
        m1.setNextMessage(m2);
        return m1;
    }
}
