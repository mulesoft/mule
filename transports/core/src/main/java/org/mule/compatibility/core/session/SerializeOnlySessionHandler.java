/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.session;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_SESSION_PROPERTY;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.session.DefaultMuleSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A session handler used to store and retrieve session information on an event. The MuleSession information is stored as a header
 * on the message (does not support Tcp, Udp, etc. unless the Message object is serialised across the wire). The session is stored
 * in the "MULE_SESSION" property as an array of bytes (byte[])
 */
public class SerializeOnlySessionHandler extends AbstractSessionHandler {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public MuleSession retrieveSessionInfoFromMessage(InternalMessage message, MuleContext muleContext) throws MuleException {
    MuleSession session = null;
    byte[] serializedSession = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);

    if (serializedSession != null) {
      session = deserialize(message, serializedSession, muleContext);
    }
    return session;
  }

  @Override
  public InternalMessage storeSessionInfoToMessage(MuleSession session, InternalMessage message, MuleContext context)
      throws MuleException {
    byte[] serializedSession = context.getObjectSerializer().serialize(removeNonSerializableProperties(session, context));

    if (logger.isDebugEnabled()) {
      logger.debug("Adding serialized Session header to message: " + serializedSession);
    }
    return InternalMessage.builder(message).addOutboundProperty(MULE_SESSION_PROPERTY, serializedSession).build();
  }

  protected MuleSession removeNonSerializableProperties(final MuleSession session, final MuleContext muleContext) {
    DefaultMuleSession copy = new DefaultMuleSession(session);
    copy.removeNonSerializableProperties();
    return copy;
  }

}
