/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.security.Authentication;

import java.util.Map;

import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPAuthentication implements Authentication {

  private boolean authenticated;
  private String userName;
  private PgpMessage message;
  private PGPPublicKey publicKey;
  transient private Event event;

  public PGPAuthentication(String userName, PgpMessage message) {
    this(userName, message, null);
  }

  public PGPAuthentication(String userName, PgpMessage message, Event event) {
    this.authenticated = false;
    this.userName = userName;
    this.message = message;
  }

  @Override
  public void setAuthenticated(boolean b) {
    authenticated = b;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public Object getCredentials() {
    return message;
  }

  public Object getDetails() {
    return publicKey;
  }

  protected void setDetails(PGPPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public Object getPrincipal() {
    return userName;
  }

  @Override
  public Map<String, Object> getProperties() {
    // TODO
    return null;
  }

  @Override
  public void setProperties(Map<String, Object> properties) {
    // TODO
  }

  @Override
  public Event getEvent() {
    return event;
  }

  public void setEvent(Event muleEvent) {
    this.event = muleEvent;
  }
}
