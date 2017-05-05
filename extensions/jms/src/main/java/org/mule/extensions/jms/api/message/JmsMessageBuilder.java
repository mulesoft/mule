/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;


import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_CONTENT_TYPE;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_ENCODING;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSX_NAMES;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.encodeKey;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.toMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.exception.DestinationNotFoundException;
import org.mule.extensions.jms.internal.config.JmsConfig;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.nio.charset.Charset;
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
public class JmsMessageBuilder {

  private static final Logger LOGGER = getLogger(JmsMessageBuilder.class);
  public static final String BODY_CONTENT_TYPE_JMS_PROPERTY = "MM_MESSAGE_CONTENT_TYPE";
  public static final String BODY_ENCODING_JMS_PROPERTY = "MM_MESSAGE_ENCODING";

  /**
   * The body of the {@link Message}
   */
  @Parameter
  @XmlHints(allowReferences = false)
  @Content(primary = true)
  @Summary("The body of the Message")
  private TypedValue<Object> body;

  /**
   * The JMSType header of the {@link Message}
   */
  @Parameter
  @Optional
  @Summary("The JMSType identifier header of the Message")
  private String jmsType;

  /**
   * The JMSCorrelationID header of the {@link Message}
   */
  @Parameter
  @Optional
  @Summary("The JMSCorrelationID header of the Message")
  private String correlationId;

  /**
   * {@code true} if the body type should be sent as a {@link Message} property
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Whether or not the body content type should be sent as a property")
  private boolean sendContentType;

  /**
   * The content type of the {@code body}
   */
  @Parameter
  @Optional
  @DisplayName("ContentType")
  @Example(EXAMPLE_CONTENT_TYPE)
  @Summary("The content type of the message's body")
  private String outboundContentType;

  /**
   * {@code true} if the body outboundEncoding should be sent as a {@link Message} property
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Whether or not the body outboundEncoding should be sent as a Message property")
  private boolean sendEncoding;

  /**
   * The outboundEncoding of the message's {@code body}
   */
  @Parameter
  @Optional
  @DisplayName("Encoding")
  @Example(EXAMPLE_ENCODING)
  @Summary("The encoding of the message's body")
  private String outboundEncoding;

  /**
   * The JMSReplyTo header information of the {@link Destination} where
   * {@code this} {@link Message} should be replied to
   */
  @Parameter
  @Optional
  @Summary("The destination where a reply to this Message should be sent")
  private JmsDestination replyTo;

  /**
   * The custom user properties that should be set to this {@link Message}
   */
  @Content
  @Parameter
  @Optional
  @NullSafe
  @DisplayName("User Properties")
  @Summary("The custom user properties that should be set to this Message")
  private Map<String, Object> properties;

  /**
   * The JMSX properties that should be set to this {@link Message}
   */
  @Parameter
  @Optional
  @NullSafe
  @DisplayName("JMSX Properties")
  @Summary("The JMSX properties that should be set to this Message")
  private JmsxProperties jmsxProperties;

  /**
   * Creates a {@link Message} based on the provided configurations
   * @param jmsSupport the {@link JmsSupport} used to create the JMSReplyTo {@link Destination}
   * @param session the current {@link Session}
   * @param config the current {@link JmsProducerConfig}
   * @return the {@link Message} created by the user
   * @throws JMSException if an error occurs
   */
  public Message build(JmsSupport jmsSupport, Session session, JmsConfig config)
      throws JMSException {

    Message message = toMessage(body.getValue(), session);

    setJmsCorrelationIdHeader(message);
    setJmsTypeHeader(config.getProducerConfig(), message);
    setJmsReplyToHeader(jmsSupport, session, message, replyTo);

    setJmsxProperties(message);
    setUserProperties(message);

    if (sendContentType) {
      setContentTypeProperty(message, body.getDataType());
    }
    if (sendEncoding) {
      setEncodingProperty(message, body.getDataType(), resolveOverride(config.getEncoding(), outboundEncoding));
    }

    return message;
  }

  private void setJmsReplyToHeader(JmsSupport jmsSupport, Session session, Message message, JmsDestination replyDestination) {
    try {
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

  private void setEncodingProperty(Message message, DataType dataType, String defaultEncoding) {
    try {
      message.setStringProperty(BODY_ENCODING_JMS_PROPERTY,
                                dataType.getMediaType().getCharset().map(Charset::toString).orElse(defaultEncoding));
    } catch (JMSException e) {
      LOGGER.error(format("Unable to set property [%s] of type String: ", BODY_ENCODING_JMS_PROPERTY), e);
    }
  }

  private void setContentTypeProperty(Message message, DataType dataType) {
    try {
      String value = isBlank(outboundContentType) ? dataType.getMediaType().toRfcString() : outboundContentType;
      message.setStringProperty(BODY_CONTENT_TYPE_JMS_PROPERTY, value);
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
      if (value instanceof TypedValue) {
        value = ((TypedValue) value).getValue();
      }
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

  public String getOutboundContentType() {
    return outboundContentType;
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

  public String getOutboundEncoding() {
    return outboundEncoding;
  }
}
