/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import static java.util.Collections.synchronizedMap;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyNotSerializableWasDropped;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.sessionPropertyNotSerializableWarning;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * <code>DefaultMuleSession</code> manages the interaction and distribution of events for Mule Services.
 */
@Deprecated
public final class DefaultMuleSession implements MuleSession {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3380926585676521866L;

  /**
   * logger used by this class
   */
  private static Logger logger = LoggerFactory.getLogger(DefaultMuleSession.class);

  private Map<String, TypedValue> properties;

  public DefaultMuleSession() {
    properties = synchronizedMap(new CaseInsensitiveHashMap());
  }

  public DefaultMuleSession(MuleSession session) {
    this.properties = synchronizedMap(new CaseInsensitiveHashMap());
    for (String key : session.getPropertyNamesAsSet()) {
      this.properties.put(key, createTypedValue(session, key));
    }
  }

  private TypedValue createTypedValue(MuleSession session, String key) {
    return new TypedValue(session.getProperty(key), session.getPropertyDataType(key));
  }

  /**
   * Will set a session level property. These will either be stored and retrieved using the underlying transport mechanism of
   * stored using a default mechanism
   *
   * @param key the key for the object data being stored on the session
   * @param value the value of the session data
   */
  @Override
  public void setProperty(String key, Object value) {
    if (!(value instanceof Serializable)) {
      logger.warn(sessionPropertyNotSerializableWarning(key).toString());
    }

    properties.put(key, new TypedValue(value, DataType.fromObject(value)));
  }

  @Override
  public void setProperty(String key, Object value, DataType dataType) {
    if (!(value instanceof Serializable)) {
      logger.warn(sessionPropertyNotSerializableWarning(key).toString());
    }
    properties.put(key, new TypedValue(value, dataType));
  }

  @Override
  public Set<String> getPropertyNamesAsSet() {
    return Collections.unmodifiableSet(properties.keySet());
  }

  @Override
  public void merge(MuleSession updatedSession) {
    if (updatedSession == null) {
      return;
    }
    Iterator<Entry<String, TypedValue>> propertyIterator = properties.entrySet().iterator();
    while (propertyIterator.hasNext()) {
      final Entry<String, TypedValue> entry = propertyIterator.next();
      if (entry.getValue().getValue() instanceof Serializable) {
        propertyIterator.remove();
      }
    }
    for (String updatedPropertyKey : updatedSession.getPropertyNamesAsSet()) {
      this.properties.put(updatedPropertyKey, createTypedValue(updatedSession, updatedPropertyKey));
    }
  }

  public Map<String, Object> getProperties() {
    Map<String, Object> result = new HashMap<>();
    for (String key : properties.keySet()) {
      TypedValue typedValue = properties.get(key);
      result.put(key, typedValue.getValue());
    }

    return result;
  }

  public Map<String, TypedValue> getExtendedProperties() {
    return properties;
  }

  public void removeNonSerializableProperties() {
    Iterator<Entry<String, TypedValue>> propertyIterator = properties.entrySet().iterator();
    while (propertyIterator.hasNext()) {
      final Entry<String, TypedValue> entry = propertyIterator.next();
      if (!(entry.getValue().getValue() instanceof Serializable)) {
        logger.warn(propertyNotSerializableWasDropped(entry.getKey()).toString());
        propertyIterator.remove();
      }
    }
  }

  @Override
  public void setProperty(String key, Serializable value) {
    setProperty(key, value, DataType.fromObject(value));
  }

  @Override
  public void setProperty(String key, Serializable value, DataType dataType) {
    properties.put(key, new TypedValue(value, dataType));
  }

  @Override
  public Object getProperty(String key) {
    TypedValue typedValue = properties.get(key);
    return typedValue == null ? null : typedValue.getValue();
  }

  @Override
  public Object removeProperty(String key) {
    return properties.remove(key);
  }

  // //////////////////////////
  // Serialization methods
  // //////////////////////////

  private void writeObject(ObjectOutputStream out) throws IOException {
    // Temporally replaces the properties to write only serializable values into the stream
    DefaultMuleSession copy = new DefaultMuleSession(this);
    copy.removeNonSerializableProperties();
    Map<String, TypedValue> backupProperties = properties;
    try {
      properties = copy.properties;
      out.defaultWriteObject();
    } finally {
      properties = backupProperties;
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

  @Override
  public void clearProperties() {
    properties.clear();
  }

  @Override
  public DataType getPropertyDataType(String name) {
    TypedValue typedValue = properties.get(name);

    return typedValue == null ? null : typedValue.getDataType();
  }
}
