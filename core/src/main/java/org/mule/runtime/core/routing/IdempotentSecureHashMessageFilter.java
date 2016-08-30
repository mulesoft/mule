/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <code>IdempotentSecureHashMessageFilter</code> ensures that only unique messages are received by a service. It does this by
 * calculating the SHA-256 hash of the message itself. This provides a value with an infinitesimally small chance of a collision.
 * This can be used to filter message duplicates. Please keep in mind that the hash is calculated over the entire byte array
 * representing the message, so any leading or trailing spaces or extraneous bytes (like padding) can produce different hash
 * values for the same semantic message content. Care should be taken to ensure that messages do not contain extraneous bytes.
 * This class is useful when the message does not support unique identifiers.
 */

public class IdempotentSecureHashMessageFilter extends IdempotentMessageFilter {

  private String messageDigestAlgorithm = "SHA-256";

  private final SerializableToByteArray objectToByteArray = new SerializableToByteArray();
  private final ByteArrayToHexString byteArrayToHexString = new ByteArrayToHexString();

  @Override
  protected String getIdForEvent(MuleEvent event) throws MessagingException {
    try {
      Object payload = event.getMessage().getPayload();
      byte[] bytes = (byte[]) objectToByteArray.transform(payload);
      MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
      byte[] digestedBytes = md.digest(bytes);
      return (String) byteArrayToHexString.transform(digestedBytes);
    } catch (NoSuchAlgorithmException nsa) {
      throw new RoutingException(event, this, nsa);
    } catch (TransformerException te) {
      throw new RoutingException(event, this, te);
    }
  }

  public String getMessageDigestAlgorithm() {
    return messageDigestAlgorithm;
  }

  public void setMessageDigestAlgorithm(String messageDigestAlgorithm) {
    this.messageDigestAlgorithm = messageDigestAlgorithm;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    objectToByteArray.setMuleContext(muleContext);
    byteArrayToHexString.setMuleContext(muleContext);
  }
}
