/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.mule.runtime.core.api.event.CoreEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ExceptionMessage</code> is used by the DefaultServiceExceptionStrategy for wrapping an exception with a message to send
 * via an endpointUri.
 */
public class ExceptionMessage implements Serializable {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -538516243574950621L;

  private static final Logger logger = LoggerFactory.getLogger(ExceptionMessage.class);

  // This object uses custom serialization via the writeObject() method
  private transient Object payload;
  // This object uses custom serialization via the writeObject() method
  private transient Throwable exception;

  protected Map<String, Object> properties;
  private String componentName;
  private String connectorName;
  private Date timeStamp;

  public ExceptionMessage(CoreEvent event, Throwable exception, String componentName, String connectorName) {
    this.payload = event.getMessage().getPayload().getValue();
    properties = new HashMap<>();
    this.exception = exception;
    timeStamp = new Date();
    this.componentName = componentName;
    if (connectorName != null) {
      this.connectorName = connectorName.toString();
    }

    for (Object element : ((InternalMessage) event.getMessage()).getOutboundPropertyNames()) {
      String propertyKey = (String) element;
      setProperty(propertyKey, ((InternalMessage) event.getMessage()).getOutboundProperty(propertyKey));
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    try {
      out.writeObject(exception);
    } catch (NotSerializableException e) {
      logger.warn("Exception " + exception.getClass().getName()
          + " is not serializable and will be lost when sending ExceptionMessage over the wire: " + e.getMessage());
    }
    try {
      out.writeObject(payload);
    } catch (NotSerializableException e) {
      logger.warn("Payload " + payload.getClass().getName()
          + " is not serializable and will be lost when sending ExceptionMessage over the wire: " + e.getMessage());
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    try {
      exception = (Throwable) in.readObject();
    } catch (Exception e) {
      // ignore
    }
    try {
      payload = in.readObject();
    } catch (Exception e) {
      // ignore
    }
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

  /**
   * @return the current message
   */
  public Object getPayload() {
    return payload;
  }


  /**
   * Adds a map of properties to associated with this message
   *
   * @param properties the properties add to this message
   */
  public void addProperties(Map<String, Object> properties) {
    this.properties.putAll(properties);
  }

  /**
   * Removes all properties on this message
   */
  public void clearProperties() {
    properties.clear();
  }

  /**
   * Returns a map of all properties on this message
   *
   * @return a map of all properties on this message
   */
  public Map getProperties() {
    return properties;
  }

  public void setProperty(String key, Object value) {
    properties.put(key, value);
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }

  public String getComponentName() {
    return componentName;
  }

  public String getConnectorName() {
    return connectorName;
  }

  public Date getTimeStamp() {
    return timeStamp;
  }

  public Throwable getException() {
    return exception;
  }

  @Override
  public String toString() {
    return "ExceptionMessage{" + "payload=" + getPayload() + ", context=" + properties + "exception=" + exception
        + ", componentName='" + componentName + "'" + ", connectorName=" + connectorName + ", timeStamp=" + timeStamp + "}";
  }
}
