/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;


import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.operation.JmsOperationCommons.resolveOverride;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSX_NAMES;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.encodeKey;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.toMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.exception.DestinationNotFoundException;
import org.mule.extensions.jms.api.message.JmsxProperties;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;

/**
 * Enables the creation of an outgoing {@link Message}.
 * Users must use this builder to create a message instance.
 *
 * @since 4.0
 */
public class MessageBuilder {

  private static final Logger LOGGER = getLogger(MessageBuilder.class);
  public static final String BODY_CONTENT_TYPE_JMS_PROPERTY = "MM_MESSAGE_CONTENT_TYPE";
  public static final String BODY_ENCODING_JMS_PROPERTY = "MM_MESSAGE_ENCODING";

  /**
   * the body of the {@link Message}
   */
  @Parameter
  @XmlHints(allowReferences = false)
  @Content(primary = true)
  private Object body;

  /**
   * the JMSType header of the {@link Message}
   */
  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private String jmsType;

  /**
   * the JMSCorrelationID header of the {@link Message}
   */
  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private String correlationId;

  /**
   * {@code true} if the body type should be sent as a {@link Message} property
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean sendContentType;

  /**
   * the body type of the {@code body}
   */
  @Parameter
  @Optional
  @DisplayName("ContentType")
  private String contentType;

  /**
   * {@code true} if the body encoding should be sent as a {@link Message} property
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean sendEncoding;

  /**
   * the encoding of the message's {@code body}
   */
  @Parameter
  @Optional
  @DisplayName("Encoding")
  private String encoding;

  /**
   * the JMSReplyTo header information of the {@link Destination} where
   * {@code this} {@link Message} should be replied to
   */
  @Parameter
  @Optional
  @Summary("The destination where a reply to the message should be sent")
  private JmsDestination replyTo;

  /**
   * the JMSX properties that should be set to this {@link Message}
   */
  @Parameter
  @Optional
  @NullSafe
  private JmsxProperties jmsxProperties;

  /**
   * the custom user properties that should be set to this {@link Message}
   */
  @Parameter
  @Optional
  @NullSafe
  @Content
  private Map<String, Object> properties;

  /**
   * Creates a {@link Message} based on the provided configurations
   * @param jmsSupport the {@link JmsSupport} used to create the JMSReplyTo {@link Destination}
   * @param session the current {@link Session}
   * @param config the current {@link JmsProducerConfig}
   * @return the {@link Message} created by the user
   * @throws JMSException if an error occurs
   */
  Message build(JmsSupport jmsSupport, Session session, JmsConfig config)
      throws JMSException {

    Message message = toMessage(body, session);

    setJmsCorrelationIdHeader(message);
    setJmsTypeHeader(config.getProducerConfig(), message);
    setJmsReplyToHeader(jmsSupport, session, message, replyTo);

    setJmsxProperties(message);
    setUserProperties(message);

    if (sendContentType) {
      setContentTypeProperty(config, message);
    }
    if (sendEncoding) {
      setEncodingProperty(config, message);
    }

    return message;
  }

  private void setJmsReplyToHeader(JmsSupport jmsSupport, Session session, Message message, JmsDestination replyDestination) {
    try {

      // TODO NullSafe should not propagate default objects unless inner NullSafe is declared

      if (replyDestination != null &&
          !isBlank(replyDestination.getDestination())) {
        Destination destination = jmsSupport.createDestination(session, replyDestination.getDestination(),
                                                               replyDestination.getDestinationType().isTopic());
        message.setJMSReplyTo(destination);
      }
    } catch (DestinationNotFoundException | JMSException e) {
      LOGGER.error("Unable to set JMSReplyTo header: ", e);
    }
  }

  private void setEncodingProperty(JmsConfig config, Message message) {
    try {
      message.setStringProperty(BODY_ENCODING_JMS_PROPERTY, resolveOverride(config.getEncoding(), encoding));
    } catch (JMSException e) {
      LOGGER.error(format("Unable to set property [%s] of type String: ", BODY_ENCODING_JMS_PROPERTY), e);
    }
  }

  private void setContentTypeProperty(JmsConfig config, Message message) {
    try {
      message.setStringProperty(BODY_CONTENT_TYPE_JMS_PROPERTY, resolveOverride(config.getContentType(), contentType));
    } catch (JMSException e) {
      LOGGER.error(format("Unable to set property [%s] of type String: ", BODY_CONTENT_TYPE_JMS_PROPERTY), e);
    }
  }

  private void setJmsxProperties(final Message message) {
    jmsxProperties.asMap().entrySet().stream()
        .filter(e -> e.getValue() != null)
        .forEach(e -> setJmsPropertySanitizeKeyIfNecessary(message, e.getKey(), e.getValue()));
  }

  private void setUserProperties(final Message message) {
    properties.keySet().stream()
        .filter(key -> !isBlank(key) && !JMSX_NAMES.contains(key))
        .forEach(key -> setJmsPropertySanitizeKeyIfNecessary(message, key, properties.get(key)));
  }


  private void setJmsPropertySanitizeKeyIfNecessary(Message msg, String key, Object value) {
    try {
      // sanitize key as JMS Property Name
      key = encodeKey(key);
      msg.setObjectProperty(key, value);
    } catch (JMSException e) {
      // Various JMS servers have slightly different rules to what
      // can be set as an object property on the message; therefore
      // we have to take a hit n' hope approach
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Unable to set property [%s] of type [%s]: ", key, value.getClass().getSimpleName()), e);
      }
    }
  }


  private void setJmsTypeHeader(JmsProducerConfig config, Message message) {
    try {
      String type = resolveOverride(config.getJmsType(), jmsType);
      if (!isBlank(type)) {
        message.setJMSType(type);
      }
    } catch (JMSException e) {
      LOGGER.error("An error occurred while setting the JMSType property: %s", e);
    }
  }

  private void setJmsCorrelationIdHeader(Message message) {
    try {
      if (!isBlank(correlationId)) {
        message.setJMSCorrelationID(correlationId);
      }
    } catch (JMSException e) {
      LOGGER.error("An error occurred while setting the JMSCorrelationID property: %s", e);
    }
  }

  public Object getBody() {
    return body;
  }

  public boolean isSendContentType() {
    return sendContentType;
  }

  public String getContentType() {
    return contentType;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public String getJmsType() {
    return jmsType;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public JmsxProperties getJmsxProperties() {
    return jmsxProperties;
  }

  public JmsDestination getReplyTo() {
    return replyTo;
  }

  public boolean isSendEncoding() {
    return sendEncoding;
  }

  public String getEncoding() {
    return encoding;
  }
}
