/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;

import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXAppID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXConsumerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXDeliveryCount;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupSeq;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXProducerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXRcvTimestamp;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXUserID;
import org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.jms.Message;

/**
 * JMS reserves the 'JMSX' property name prefix for JMS defined properties. Here we
 * define the set of 'well known' properties of JMS.
 *
 * JMSX properties 'set by provider on send' are available to both the producer and
 * the consumers of the message. If they are not present in a message, they are treated
 * like any other absent property.
 *
 *
 * @since 4.0
 */
public final class JmsxProperties {

  /**
   * The identity of the user sending the message
   */
  @Parameter
  @Optional
  private String jmsxUserID;

  /**
   * The identity of the application sending the message
   */
  @Parameter
  @Optional
  private String jmsxAppID;

  /**
   * The number of message delivery attempts
   */
  @Parameter
  @Optional
  private Integer jmsxDeliveryCount;

  /**
   * The identity of the message group this message is part of
   */
  @Parameter
  @Optional
  private String jmsxGroupID;

  /**
   * The sequence number of this message within the group
   */
  @Parameter
  @Optional
  private Integer jmsxGroupSeq;

  /**
   * The transaction identifier of the transaction within which this message was produced
   */
  @Parameter
  @Optional
  private String jmsxProducerTXID;

  /**
   * The transaction identifier of the transaction within which this message was consumed
   */
  @Parameter
  @Optional
  private String jmsxConsumerTXID;

  /**
   * The time JMS delivered the message to the consumer
   */
  @Parameter
  @Optional
  private Long jmsxRcvTimestamp;

  public JmsxProperties() {}

  public JmsxProperties(String JMSXUserID, String JMSXAppID, Integer JMSXDeliveryCount, String JMSXGroupID, Integer JMSXGroupSeq,
                        String JMSXProducerTXID, String JMSXConsumerTXID, Long JMSXRcvTimestamp) {
    this.jmsxUserID = JMSXUserID;
    this.jmsxAppID = JMSXAppID;
    this.jmsxDeliveryCount = JMSXDeliveryCount;
    this.jmsxGroupID = JMSXGroupID;
    this.jmsxGroupSeq = JMSXGroupSeq;
    this.jmsxProducerTXID = JMSXProducerTXID;
    this.jmsxConsumerTXID = JMSXConsumerTXID;
    this.jmsxRcvTimestamp = JMSXRcvTimestamp;
  }

  /**
   * @return all the JMSX properties in a plain {@link Map} representation using the corresponding
   * keys as defined in {@link JMSXDefinedPropertiesNames}
   */
  public Map<String, Object> asMap() {
    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();

    addIfNotNullValue(builder, JMSXUserID, this.jmsxUserID);
    addIfNotNullValue(builder, JMSXAppID, this.jmsxAppID);
    addIfNotNullValue(builder, JMSXDeliveryCount, this.jmsxDeliveryCount);
    addIfNotNullValue(builder, JMSXGroupID, this.jmsxGroupID);
    addIfNotNullValue(builder, JMSXGroupSeq, this.jmsxGroupSeq);
    addIfNotNullValue(builder, JMSXProducerTXID, this.jmsxProducerTXID);
    addIfNotNullValue(builder, JMSXConsumerTXID, this.jmsxConsumerTXID);
    addIfNotNullValue(builder, JMSXRcvTimestamp, this.jmsxRcvTimestamp);

    return builder.build();
  }

  /**
   * @return the JMSXUserID {@link Message} property
   */
  public String getJMSXUserID() {
    return jmsxUserID;
  }

  /**
   * @return the JMSXAppID {@link Message} property
   */
  public String getJMSXAppID() {
    return jmsxAppID;
  }

  /**
   * @return the JMSXDeliveryCount {@link Message} property
   */
  public int getJMSXDeliveryCount() {
    return jmsxDeliveryCount;
  }

  /**
   * @return the JMSXGroupID {@link Message} property
   */
  public String getJMSXGroupID() {
    return jmsxGroupID;
  }

  /**
   * @return the JMSXGroupSeq {@link Message} property
   */
  public int getJMSXGroupSeq() {
    return jmsxGroupSeq;
  }

  /**
   * @return the JMSXProducerTXID {@link Message} property
   */
  public String getJMSXProducerTXID() {
    return jmsxProducerTXID;
  }

  /**
   * @return the JMSXConsumerTXID {@link Message} property
   */
  public String getJMSXConsumerTXID() {
    return jmsxConsumerTXID;
  }

  /**
   * @return the JMSXRcvTimestamp
   */
  public long getJMSXRcvTimestamp() {
    return jmsxRcvTimestamp;
  }

  private void addIfNotNullValue(ImmutableMap.Builder<String, Object> builder, String key, Object value) {
    if (value != null) {
      builder.put(key, value);
    }
  }

}
