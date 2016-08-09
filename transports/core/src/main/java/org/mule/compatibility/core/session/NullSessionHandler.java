/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.session;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.message.SessionHandler;

/**
 * A session handler that ignores any session information
 */
public class NullSessionHandler implements SessionHandler {

  @Override
  public MuleSession retrieveSessionInfoFromMessage(MuleMessage message, MuleContext muleContext) throws MuleException {
    return null;
  }

  @Override
  public MuleMessage storeSessionInfoToMessage(MuleSession session, MuleMessage message, MuleContext context)
      throws MuleException {
    return message;
  }

}
