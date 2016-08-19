/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.message;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.Correlation;

import java.io.Serializable;
import java.util.Set;

import javax.activation.DataHandler;

/**
 * Adds functionality that was available in {@link MuleMessage} in Mule 3.
 *
 * @since 4.0
 */
public class MuleCompatibilityMessage implements MuleMessage {

  private MuleMessage inner;
  private String correlationId;
  private Correlation correlation;

  public MuleCompatibilityMessage(MuleMessage inner, Correlation correlation, String correlationId) {
    this.inner = inner;
    this.correlation = correlation;
    this.correlationId = correlationId;
  }

  @Override
  public Attributes getAttributes() {
    return inner.getAttributes();
  }

  @Override
  public DataType getDataType() {
    return inner.getDataType();
  }

  @Override
  public DataHandler getInboundAttachment(String name) {
    return inner.getInboundAttachment(name);
  }

  @Override
  public <T extends Serializable> T getOutboundProperty(String name) {
    return inner.getOutboundProperty(name);
  }

  @Override
  public <T extends Serializable> T getOutboundProperty(String name, T defaultValue) {
    return inner.getOutboundProperty(name, defaultValue);
  }

  @Override
  public DataHandler getOutboundAttachment(String name) {
    return inner.getOutboundAttachment(name);
  }

  @Override
  public Set<String> getInboundAttachmentNames() {
    return inner.getInboundAttachmentNames();
  }

  @Override
  public Set<String> getOutboundAttachmentNames() {
    return inner.getOutboundAttachmentNames();
  }

  @Override
  public DataType getOutboundPropertyDataType(String name) {
    return inner.getOutboundPropertyDataType(name);
  }

  @Override
  public Set<String> getOutboundPropertyNames() {
    return inner.getOutboundPropertyNames();
  }

  @Override
  public <T extends Serializable> T getInboundProperty(String name) {
    return inner.getInboundProperty(name);
  }

  @Override
  public <T extends Serializable> T getInboundProperty(String name, T defaultValue) {
    return inner.getInboundProperty(name, defaultValue);
  }

  @Override
  public ExceptionPayload getExceptionPayload() {
    return inner.getExceptionPayload();
  }

  @Override
  public DataType getInboundPropertyDataType(String name) {
    return inner.getInboundPropertyDataType(name);
  }

  @Override
  public Set<String> getInboundPropertyNames() {
    return inner.getInboundPropertyNames();
  }

  @Override
  public <T> T getPayload() {
    return inner.getPayload();
  }

  public Correlation getCorrelation() {
    return correlation;
  }

  public String getCorrelationId() {
    return this.correlationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    MuleCompatibilityMessage that = (MuleCompatibilityMessage) o;

    if (!this.inner.equals(that.inner)) {
      return false;
    }
    return this.correlation.equals(that.correlation);
  }

  @Override
  public int hashCode() {
    int result = this.inner.hashCode();
    result = 31 * result + this.correlation.hashCode();
    return result;
  }
}
