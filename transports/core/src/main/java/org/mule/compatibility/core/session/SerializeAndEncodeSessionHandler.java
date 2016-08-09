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
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.model.SessionException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.Base64;

import java.io.IOException;

/**
 * A session handler used to store and retrieve session information on an event. The DefaultMuleSession information is stored as a
 * header on the message (does not support Tcp, Udp, etc. unless the MuleMessage object is serialised across the wire). The
 * session is stored in the "MULE_SESSION" property as Base64 encoded byte array.
 */
public class SerializeAndEncodeSessionHandler extends SerializeOnlySessionHandler {

  @Override
  public MuleSession retrieveSessionInfoFromMessage(MuleMessage message, MuleContext muleContext) throws MuleException {
    MuleSession session = null;
    String serializedEncodedSession = message.getInboundProperty(MULE_SESSION_PROPERTY);

    if (serializedEncodedSession != null) {
      byte[] serializedSession = Base64.decodeWithoutUnzipping(serializedEncodedSession);
      if (serializedSession != null) {
        session = deserialize(message, serializedSession, muleContext);
      }
    }
    return session;
  }

  @Override
  public MuleMessage storeSessionInfoToMessage(MuleSession session, MuleMessage message, MuleContext context)
      throws MuleException {
    session = removeNonSerializableProperties(session, context);
    byte[] serializedSession = serialize(message, session, context);

    String serializedEncodedSession;
    try {
      serializedEncodedSession = Base64.encodeBytes(serializedSession, Base64.DONT_BREAK_LINES);
    } catch (IOException e) {
      throw new SessionException(MessageFactory.createStaticMessage("Unable to serialize MuleSession"), e);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Adding serialized and base64-encoded Session header to message: " + serializedEncodedSession);
    }
    return MuleMessage.builder(message).addOutboundProperty(MULE_SESSION_PROPERTY, serializedEncodedSession).build();
  }
}
