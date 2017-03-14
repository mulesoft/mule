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
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_CONTENT_TYPE;
import static org.mule.extensions.jms.internal.common.JmsCommons.EXAMPLE_ENCODING;
import static org.mule.extensions.jms.internal.common.JmsCommons.evaluateMessageAck;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageContentType;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveMessageEncoding;
import static org.mule.extensions.jms.internal.common.JmsCommons.resolveOverride;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.JmsSessionManager;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.config.JmsConsumerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.exception.JmsExtensionException;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.api.message.MessageBuilder;
import org.mule.extensions.jms.internal.consume.JmsMessageConsumer;
import org.mule.extensions.jms.internal.message.JmsResultFactory;
import org.mule.extensions.jms.internal.metadata.JmsOutputResolver;
import org.mule.extensions.jms.internal.publish.JmsPublishParameters;
import org.mule.extensions.jms.internal.support.Jms102bSupport;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
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
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.slf4j.Logger;

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
//TODO - MULE-11964 : Review JMS Listener's consumers usage implementation
public class JmsListener extends Source<Object, JmsAttributes> {

  private static final Logger LOGGER = getLogger(JmsListener.class);
  private static final String REPLY_TO_DESTINATION = "REPLY_TO_DESTINATION";
  private final JmsResultFactory resultFactory = new JmsResultFactory();

  @Inject
  private JmsSessionManager sessionManager;

  @UseConfig
  private JmsConfig config;

  @Connection
  private JmsConnection connection;

  private JmsSession session;

  private JmsSupport jmsSupport;

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

  private JmsListenerLock jmsLock;

  @Override
  public void onStart(SourceCallback<Object, JmsAttributes> sourceCallback) throws MuleException {
    jmsLock = synchronous ? new DefaultJmsListenerLock() : new NullJmsListenerLock();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Starting message listener");
    }

    JmsConsumerConfig consumerConfig = config.getConsumerConfig();
    ackMode = resolveOverride(consumerConfig.getAckMode(), ackMode);
    selector = resolveOverride(consumerConfig.getSelector(), selector);
    consumerType = resolveOverride(config.getConsumerConfig().getConsumerType(), consumerType);

    try {
      session = connection.createSession(ackMode, consumerType.isTopic());
      jmsSupport = connection.getJmsSupport();
      final Destination jmsDestination = jmsSupport.createDestination(session.get(), destination, consumerType.isTopic());
      final JmsMessageConsumer consumer = connection.createConsumer(session.get(), jmsDestination, selector, consumerType);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Starting Message listener on destination [%s] of type [%s]",
                            destination, consumerType.isTopic() ? "TOPIC" : "QUEUE"));
      }
      consumer.listen(message -> {
        SourceCallbackContext context = sourceCallback.createContext();

        if (message != null) {
          evaluateAckAction(sourceCallback, session, message, jmsLock);
          encoding = resolveEncoding(message);
          contentType = resolveContentType(message);
          saveReplyToDestination(sourceCallback, message, context);
        }

        produceMessageResult(sourceCallback, jmsSupport, session, message, context);
        waitForMessageToBeProcesed();
      });

    } catch (Exception e) {
      LOGGER.error("An error occurred while consuming a message: ", e);
      sourceCallback.onSourceException(new JmsExtensionException(e, "An error occurred while consuming a message: "));
    }
  }

  @Override
  public void onStop() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stopping JMSSubscriber source");
    }

    jmsLock.unlock();

    try {
      if (ackMode.equals(AUTO) || ackMode.equals(DUPS_OK) || ackMode.equals(MANUAL)) {
        session.get().recover();
      }
    } catch (JMSException e) {
      throw new JmsExtensionException(e, "A problem occurred recovering the session before returning the session to the pool");
    }
  }

  @OnSuccess
  public void onSuccess(@Optional @NullSafe JmsListenerResponseBuilder response,
                        SourceCallbackContext callbackContext) {
    jmsLock.unlock();
    Destination replyTo = callbackContext.getVariable(REPLY_TO_DESTINATION);
    if (replyTo != null) {
      doReply(response.getMessageBuilder(), response.getOverrides(), callbackContext, replyTo);
    }
  }

  @OnError
  public void onError(Error error) {
    if (ackMode.equals(AUTO) || ackMode.equals(DUPS_OK)) {
      jmsLock.unlockWithFailure(error);
    } else {
      jmsLock.unlock();
    }
  }

  private void doReply(MessageBuilder messageBuilder, JmsPublishParameters overrides,
                       SourceCallbackContext callbackContext, Destination replyTo) {
    try {
      boolean replyToTopic = replyDestinationIsTopic(replyTo);
      String destinationName = replyToTopic ? ((Topic) replyTo).getTopicName() : ((Queue) replyTo).getQueueName();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Begin reply to destination [%s] of type [%s]", destinationName, replyToTopic ? "TOPIC" : "QUEUE"));
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

  private void produceMessageResult(SourceCallback<Object, JmsAttributes> sourceCallback, JmsSupport jmsSupport,
                                    JmsSession session, Message message, SourceCallbackContext context) {
    try {

      Result<Object, JmsAttributes> result = resultFactory.createResult(message, jmsSupport.getSpecification(), contentType,
                                                                        encoding,
                                                                        session.getAckId());
      sourceCallback.handle(result, context);

    } catch (Exception e) {
      LOGGER.error("An error occurred while creating the initial message", e);
      sourceCallback.onSourceException(e);
    }
  }

  private void evaluateAckAction(SourceCallback<Object, JmsAttributes> sourceCallback, JmsSession session, Message message,
                                 JmsListenerLock jmsLock) {
    try {
      evaluateMessageAck(ackMode, session, message, sessionManager, jmsLock);
    } catch (JMSException e) {
      LOGGER.error("An error occurred while processing an incoming message: ", e);
      sourceCallback.onSourceException(e);
    }
  }

  private String resolveContentType(Message message) {
    // If no explicit content type was provided to the operation, fallback to the
    // one communicated in the message properties. Finally if no property was set,
    // use the default one provided by the config
    return resolveOverride(resolveMessageContentType(message, config.getContentType()), contentType);
  }

  private String resolveEncoding(Message message) {
    // If no explicit content type was provided to the operation, fallback to the
    // one communicated in the message properties. Finally if no property was set,
    // use the default one provided by the config
    return resolveOverride(resolveMessageEncoding(message, config.getEncoding()), encoding);
  }

  private void saveReplyToDestination(SourceCallback<Object, JmsAttributes> sourceCallback, Message message,
                                      SourceCallbackContext context) {
    try {
      Destination replyTo = message.getJMSReplyTo();
      if (replyTo != null) {
        context.addVariable(REPLY_TO_DESTINATION, replyTo);
      }
    } catch (JMSException e) {
      LOGGER.error("An error occurred while obtaining the ReplyTo destination: ", e);
      sourceCallback
          .onSourceException(new JmsExtensionException(e, "An error occurred while obtaining the ReplyTo destination: "));
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

  private void waitForMessageToBeProcesed() {
    if (synchronous) {
      jmsLock.lock();
    }
  }
}
