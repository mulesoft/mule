/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jbi.components;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * A Jbi MEssage exchange listener
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MessageExchangeListener {

    public void onExchange(MessageExchange me) throws MessagingException;
}
