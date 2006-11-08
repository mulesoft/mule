/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.space;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.space.UMOSpaceException;

/**
 * Is thrown when an error occurs during begin, commit or rollback of a transaction.
 * There will always be a cause exception.
 */
public class SpaceTransactionException extends UMOSpaceException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8131578773037713193L;

    public SpaceTransactionException(Throwable cause)
    {
        // TODO better message
        super(new Message(Messages.FAILED_TO_INVOKE_X, "transaction"), cause);
    }
}
