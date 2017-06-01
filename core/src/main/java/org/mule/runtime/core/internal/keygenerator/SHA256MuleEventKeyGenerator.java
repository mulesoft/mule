/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.keygenerator;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventKeyGenerator;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link org.mule.runtime.core.api.MuleEventKeyGenerator} applying SHA-256 digest to the event's message payload.
 */
public class SHA256MuleEventKeyGenerator implements MuleEventKeyGenerator, MuleContextAware {

  private static final Logger logger = LoggerFactory.getLogger(SHA256MuleEventKeyGenerator.class);
  private MuleContext muleContext;

  @Override
  public Serializable generateKey(Event event) throws NotSerializableException {
    try {
      byte[] bytesOfMessage = event.getMessageAsBytes(muleContext);
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      String key = StringUtils.toHexString(md.digest(bytesOfMessage));

      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Generated key for event: %s key: %s", event, key));
      }

      return key;
    } catch (Exception e) {
      // TODO: The exception may not necessarily be caused by a serialization problem, but we still throw
      // NotSerializableException to keep backwards compatibility. The interface needs to be changed.

      NotSerializableException notSerializableException = new NotSerializableException(e.getMessage());
      notSerializableException.initCause(e);

      throw notSerializableException;
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
