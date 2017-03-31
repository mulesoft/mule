/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleSession;

/**
 * An interface used for reading and writing session information to and from the current message.
 */
public interface SessionHandler {

  Message storeSessionInfoToMessage(MuleSession session, Message message, MuleContext context)
      throws MuleException;

  MuleSession retrieveSessionInfoFromMessage(Message message, MuleContext muleContext) throws MuleException;

}
