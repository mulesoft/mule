/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.source;

import static java.lang.String.format;
import static org.mule.extensions.jms.api.config.AckMode.AUTO;
import static org.mule.extensions.jms.api.config.AckMode.DUPS_OK;
import static org.mule.extensions.jms.api.config.AckMode.MANUAL;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_CONTENT_TYPE;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_ENCODING;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.JmsSessionManager;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.config.JmsConsumerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.MessageBuilder;
import org.mule.extensions.jms.api.publish.JmsPublishParameters;
import org.mule.extensions.jms.internal.consume.JmsMessageConsumer;
import org.mule.extensions.jms.internal.metadata.JmsOutputResolver;
import org.mule.extensions.jms.internal.support.Jms102bSupport;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * JMS Subscriber for {@link Destination}s, allows to listen
 * for incoming {@link Message}s
 *
 * @since 4.0
 */
@Alias("listener")
@EmitsResponse
@MetadataScope(outputResolver = JmsOutputResolver.class)
public class JmsListener extends Source<Object, JmsAttributes> {

  private static final Logger LOGGER = getLogger(JmsListener.class);
  private static final String TOPIC = "TOPIC";
  private static final String QUEUE = "QUEUE";
  static final String REPLY_TO_DESTINATION_VAR = "REPLY_TO_DESTINATION";
  static final String JMS_LOCK_VAR = "JMS_LOCK";
  static final String JMS_SESSION_VAR = "JMS_SESSION";

  @Inject
  private JmsSessionManager sessionManager;

  @Config
  private JmsConfig config;

  @Connection
  private JmsConnection connection;

  private JmsSupport jmsSupport;

  /**
   * List to save all the created {@link JmsSession} and {@link JmsListenerLock} by this listener.
   */
  private final List<Pair<JmsSession, JmsListenerLock>> sessions = new ArrayList<>();

  /**
   * The name of the Destination from where the Message should be consumed
   */
  @Parameter
  @XmlHints(allowReferences = false)
  private String destination;

  /**
   * The Type of the Consumer that should be used for the provided destination
   */
  @Parameter
  @Optional
  private ConsumerType consumerType;

  /**
   * The Session ACK mode to use when consuming a message
   */
  @Parameter
  @Optional
  private AckMode ackMode;

  /**
   * JMS selector to be used for filtering incoming messages
   */
  @Parameter
  @Optional
  private String selector;

  /**
   * The content type of the message body
   */
  @Parameter
  @Optional
  @Example(EXAMPLE_CONTENT_TYPE)
  private String contentType;

  /**
   * The encoding of the message body
   */
  @Parameter
  @Optional
  @Example(EXAMPLE_ENCODING)
  private String encoding;

  /**
   * This makes the message listener to work synchronously, only one message at a time will be consumed, delivered
   * and waited to be processed in the flow.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean synchronous;

  /**
   * The number of concurrent consumers that will be used to receive JMS Messages
   */
  @Parameter
  @Optional(defaultValue = "4")
  private int numberOfConsumers;


  @Override
  public void onStart(SourceCallback<Object, JmsAttributes> sourceCallback) throws MuleException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Starting JMS Message Listener");
    }

    JmsConsumerConfig consumerConfig = config.getConsumerConfig();
    ackMode = resolveOverride(consumerConfig.getAckMode(), ackMode);
    selector = resolveOverride(consumerConfig.getSelector(), selector);
    consumerType = resolveOverride(config.getConsumerConfig().getConsumerType(), consumerType);
    jmsSupport = connection.getJmsSupport();

    JmsMessageListenerFactory messageListenerFactory =
        new JmsMessageListenerFactory(ackMode, encoding, contentType, config, sessionManager, jmsSupport, sourceCallback);

    validateNumberOfConsumers(numberOfConsumers);

    LOGGER.debug("Starting JMS Listener with [" + numberOfConsumers + "] consumers");

    try {
      for (int i = 0; i < numberOfConsumers; i++) {
        JmsSession session = connection.createSession(ackMode, consumerType.isTopic());

        final Destination jmsDestination = jmsSupport.createDestination(session.get(), destination, consumerType.isTopic());
        final JmsMessageConsumer consumer = connection.createConsumer(session.get(), jmsDestination, selector, consumerType);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("Starting Message listener on destination [%s] of type [%s]",
                              destination, consumerType.isTopic() ? TOPIC : QUEUE));
        }

        JmsListenerLock jmsLock = createJmsLock();
        sessions.add(new Pair<>(session, jmsLock));
        consumer.listen(messageListenerFactory.createMessageListener(session, jmsLock));
      }
    } catch (Exception e) {
      LOGGER.error("An error occurred while consuming a message: ", e);
      sourceCallback.onSourceException(new JmsExtensionException(e, "An error occurred while consuming a message: "));
    }
  }

  @Override
  public void onStop() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stopping JMS Message Listener");
    }

    if (synchronous) {
      sessions.stream()
          .map(Pair::getSecond)
          .forEach(JmsListenerLock::unlockWithFailure);
    } else {
      try {
        if (ackMode.equals(AUTO) || ackMode.equals(DUPS_OK) || ackMode.equals(MANUAL)) {
          for (Pair<JmsSession, JmsListenerLock> session : sessions) {
            session.getFirst().get().recover();
          }
        }
      } catch (JMSException e) {
        throw new JmsExtensionException(e, "A problem occurred recovering the session before returning the session to the pool");
      }
    }
  }

  @OnSuccess
  public void onSuccess(@Optional @NullSafe JmsListenerResponseBuilder response,
                        SourceCallbackContext callbackContext) {
    callbackContext.<JmsListenerLock>getVariable(JMS_LOCK_VAR)
        .ifPresent(JmsListenerLock::unlock);

    callbackContext.<Destination>getVariable(REPLY_TO_DESTINATION_VAR)
        .ifPresent(replyTo -> callbackContext.<JmsSession>getVariable(JMS_SESSION_VAR)
            .ifPresent(session -> doReply(response.getMessageBuilder(), response.getOverrides(), callbackContext, replyTo,
                                          session)));
  }

  @OnError
  public void onError(Error error, SourceCallbackContext callbackContext) {
    callbackContext.<JmsListenerLock>getVariable(JMS_LOCK_VAR)
        .ifPresent(jmsLock -> {
          if (ackMode.equals(AUTO) || ackMode.equals(DUPS_OK)) {
            jmsLock.unlockWithFailure(error);
          } else {
            jmsLock.unlock();
          }
        });
  }

  private void doReply(MessageBuilder messageBuilder, JmsPublishParameters overrides,
                       SourceCallbackContext callbackContext, Destination replyTo, JmsSession session) {
    try {
      boolean replyToTopic = replyDestinationIsTopic(replyTo);
      String destinationName = replyToTopic ? ((Topic) replyTo).getTopicName() : ((Queue) replyTo).getQueueName();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Begin reply to destination [%s] of type [%s]", destinationName, replyToTopic ? TOPIC : QUEUE));
      }

      Message message = messageBuilder.build(connection.getJmsSupport(), session.get(), config);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Message built, sending message to " + destinationName);
      }

      JmsSession replySession = connection.createSession(AUTO, replyToTopic);
      connection.createProducer(replySession.get(), replyTo, replyToTopic)
          .publish(message, config.getProducerConfig(), overrides);

    } catch (Exception e) {
      LOGGER.error("An error occurred during reply: ", e);
      callbackContext.getSourceCallback().onSourceException(e);
    }
  }

  private boolean replyDestinationIsTopic(Destination destination) {
    // TODO: MULE-11156 - take into account the special logic in 3.x for handling Weblogic 8.x and 9.x
    // see 'org.mule.transport.jms.weblogic.WeblogicJmsTopicResolver#isTopic'

    if (destination instanceof Topic && destination instanceof Queue
        && jmsSupport instanceof Jms102bSupport) {
      LOGGER.error(StringMessageUtils.getBoilerPlate(
                                                     "Destination implements both Queue and Topic "
                                                         + "while complying with JMS 1.0.2b specification. "
                                                         + "Please report your application server or JMS vendor name and version "
                                                         + "to http://www.mulesoft.org/jira"));
    }

    return destination instanceof Topic;
  }

  private JmsListenerLock createJmsLock() {
    return synchronous ? new DefaultJmsListenerLock() : new NullJmsListenerLock();
  }

  private void validateNumberOfConsumers(int numberOfConsumers) {
    if (numberOfConsumers < 1) {
      throw new IllegalArgumentException("Invalid number of consumers: [" + numberOfConsumers
          + "]. The number should be 1 or greater.");
    }

    if (numberOfConsumers > 1 && consumerType.isTopic()) {
      TopicConsumer topicConsumer = (TopicConsumer) consumerType;

      if (!isCapableOfMultiConsumersOnTopic(topicConsumer)) {
        throw new IllegalArgumentException("Destination [" + destination + "] is a topic, but [" + numberOfConsumers
            + "] receivers have been requested. This is only possible for 'shared' topic consumers, otherwise use 1.");
      }
    }
  }

  private boolean isCapableOfMultiConsumersOnTopic(TopicConsumer topicConsumer) {
    return jmsSupport.getSpecification().equals(JMS_2_0) && topicConsumer.isShared();
  }
}
