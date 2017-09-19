/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.el.context;

import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.privileged.event.MuleSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Session variables wrapper {@link Map} for exposing Session variables via an
 * {@link ExtendedExpressionLanguageAdaptor}
 */
public class SessionVariableMapContext extends AbstractMapContext<Object> {

  private MuleSession session;

  public SessionVariableMapContext(MuleSession session) {
    this.session = session;
  }

  @Override
  public Object doGet(String key) {
    return session.getProperty(key);
  }

  @Override
  public void doPut(String key, Object value) {
    session.setProperty(key, value);
  }

  @Override
  public void doRemove(String key) {
    session.removeProperty(key);
  }

  @Override
  public Set<String> keySet() {
    return session.getPropertyNamesAsSet();
  }

  @Override
  public void clear() {
    session.clearProperties();
  }

  @Override
  public String toString() {
    Map<String, Object> map = new HashMap<String, Object>();
    for (String key : session.getPropertyNamesAsSet()) {
      Object value = session.getProperty(key);
      map.put(key, value);
    }
    return map.toString();
  }
}
