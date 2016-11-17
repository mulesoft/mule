/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;


import static java.lang.String.format;
import static java.nio.charset.Charset.forName;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.api.destination.DestinationType.QUEUE;
import static org.mule.extensions.jms.api.destination.DestinationType.TOPIC;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.getPropertiesMap;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.toObject;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.JmsHeaders;
import org.mule.extensions.jms.api.message.JmsMessageProperties;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.util.Optional;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating an Operation {@link Result} based on a JMS {@link Message}
 *
 * @since 4.0
 */
public class JmsResultFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmsResultFactory.class);

  /**
   *
   * Creates a {@link Result} from a JMS {@link Message} based on the current configurations
   *
   * @param jmsMessage the JMS {@link Message} to convert
   * @param specification the {@link JmsSpecification} used to obtain the {@link Message}
   * @param contentType the contentType of the {@link Message} content
   * @param encoding the encoding of the {@link Message} content
   * @param ackId the ID required to perform an ACK on the {@link Session} the {@link Message} belongs to
   * @return an Operation {@link Result} containing the content, headers and properties of the original {@link Message}
   * @throws IOException
   * @throws JMSException
   */
  public Result<Object, JmsAttributes> createResult(Message jmsMessage, JmsSpecification specification, String contentType,
                                                    String encoding, Optional<String> ackId)
      throws IOException, JMSException {

    if (jmsMessage == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resulting JMS Message was [null], creating an empty result");
      }

      return Result.<Object, JmsAttributes>builder().output(null).build();
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Creating Result: specification:[%s], type:[%s], contentType:[%s], encoding:[%s]",
                          specification.getName(), jmsMessage.getClass().getSimpleName(), contentType, encoding));
    }

    Object payload = getPayload(jmsMessage, specification, encoding);
    JmsHeaders jmsHeaders = createJmsHeaders(jmsMessage, specification);
    JmsMessageProperties jmsProperties = createJmsProperties(jmsMessage);

    return Result.<Object, JmsAttributes>builder()
        .output(payload).mediaType(getMediaType(contentType, encoding))
        .attributes(createJmsAttributes(jmsHeaders, jmsProperties, ackId))
        .build();
  }

  private JmsAttributes createJmsAttributes(JmsHeaders jmsHeaders, JmsMessageProperties jmsProperties,
                                            Optional<String> ackId) {
    DefaultJmsAttributes.Builder builder = DefaultJmsAttributes.Builder.newInstance()
        .withHeaders(jmsHeaders)
        .withProperties(jmsProperties);

    ackId.ifPresent(builder::withAckId);

    return builder.build();
  }

  private JmsMessageProperties createJmsProperties(Message message) {
    return new DefaultJmsProperties(getPropertiesMap(message));
  }

  private MediaType getMediaType(String contentType, String encoding) {
    DataTypeParamsBuilder builder = DataType.builder().mediaType(contentType);
    if (encoding != null) {
      builder.charset(forName(encoding));
    }
    return builder.build().getMediaType();
  }

  private Object getPayload(Message message, JmsSpecification specification, String encoding) throws IOException, JMSException {
    if (message == null) {
      return null;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Message type received is: " + message.getClass().getSimpleName());
    }
    return toObject(message, specification, encoding);
  }

  private JmsHeaders createJmsHeaders(Message jmsMessage, JmsSpecification specification) {
    DefaultJmsHeaders.Builder headersBuilder = new DefaultJmsHeaders.Builder();
    addCorrelationProperties(jmsMessage, headersBuilder);
    addDeliveryModeProperty(jmsMessage, headersBuilder);
    addDestinationProperty(jmsMessage, headersBuilder);
    addExpirationProperty(jmsMessage, headersBuilder);
    addMessageIdProperty(jmsMessage, headersBuilder);
    addPriorityProperty(jmsMessage, headersBuilder);
    addRedeliveredProperty(jmsMessage, headersBuilder);
    addJMSReplyTo(jmsMessage, headersBuilder);
    addTimestampProperty(jmsMessage, headersBuilder);
    addTypeProperty(jmsMessage, headersBuilder);

    if (specification.equals(JMS_2_0)) {
      addDeliveryTimeProperty(jmsMessage, headersBuilder);
    }

    return headersBuilder.build();
  }

  private void addDeliveryTimeProperty(Message jmsMessage, DefaultJmsHeaders.Builder headersBuilder) {
    addHeader(jmsMessage::getJMSDeliveryTime, (value) -> headersBuilder.setDeliveryTime((Long) value),
              "An error occurred while retrieving the JMSDeliveryTime property");
  }

  private void addTypeProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSType, (value) -> jmsHeadersBuilder.setType((String) value),
              "An error occurred while retrieving the JMSType property");
  }

  private void addTimestampProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSTimestamp, (value) -> jmsHeadersBuilder.setTimestamp((Long) value),
              "An error occurred while retrieving the JMSTimestamp property");
  }

  private void addJMSReplyTo(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSReplyTo, (value) -> jmsHeadersBuilder.setReplyTo(getDestination((Destination) value)),
              "An error occurred while retrieving the JMSReplyTo property");
  }

  private void addRedeliveredProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSRedelivered, (value) -> jmsHeadersBuilder.setRedelivered((Boolean) value),
              "An error occurred while retrieving the JMSRedelivered property");
  }

  private void addPriorityProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSPriority, (value) -> jmsHeadersBuilder.setPriority((Integer) value),
              "An error occurred while retrieving the JMSPriority property");
  }

  private void addMessageIdProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    //TODO here mule sets the MULE_MESSAGE_ID see if we have to do something
    addHeader(jmsMessage::getJMSMessageID, (value) -> jmsHeadersBuilder.setMessageId((String) value),
              "An error occurred while retrieving the JMSMessageID property");
  }

  private void addExpirationProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSExpiration, (value) -> jmsHeadersBuilder.setExpiration((Long) value),
              "An error occurred while retrieving the JMSExpiration property");
  }

  private void addDestinationProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSDestination, (value) -> jmsHeadersBuilder.setDestination(getDestination((Destination) value)),
              "An error occurred while retrieving the JMSDestination property");
  }

  private void addDeliveryModeProperty(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    addHeader(jmsMessage::getJMSDeliveryMode, (value) -> jmsHeadersBuilder.setDeliveryMode((Integer) value),
              "An error occurred while retrieving the JMSDeliveryMode property");
  }

  private void addCorrelationProperties(Message jmsMessage, DefaultJmsHeaders.Builder jmsHeadersBuilder) {
    //TODO previously here the MULE_CORRELATION_ID was set also, see what to do with that.
    addHeader(jmsMessage::getJMSCorrelationID, (value) -> jmsHeadersBuilder.setCorrelactionId((String) value),
              "An error occurred while retrieving the JMSCorrelationID property");
  }

  private void addHeader(JmsHeaderValueSupplier valueSupplier, JmsHeaderValueConsumer headerSetter, String message) {
    try {
      Object value = valueSupplier.get();
      if (value != null) {
        headerSetter.set(value);
      }
    } catch (JMSException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(message, e.getMessage());
      }
    }
  }

  private JmsDestination getDestination(Destination value) throws JMSException {
    return value instanceof Queue
        ? new JmsDestination(((Queue) value).getQueueName(), QUEUE)
        : new JmsDestination(((Topic) value).getTopicName(), TOPIC);
  }

  private interface JmsHeaderValueSupplier<T> {

    T get() throws JMSException;

  }

  private interface JmsHeaderValueConsumer<T> {

    void set(T value) throws JMSException;

  }

}
