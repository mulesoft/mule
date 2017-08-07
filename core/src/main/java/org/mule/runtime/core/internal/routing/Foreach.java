/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPIT_EXPRESSION;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Scope;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.privileged.expression.ExpressionConfig;
import org.mule.runtime.core.internal.routing.outbound.AbstractMessageSequenceSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

/**
 * The {@code foreach} {@link Processor} allows iterating over a collection payload, or any collection obtained by an expression,
 * generating a message for each element.
 * <p>
 * The number of the message being processed is stored in {@code #[mel:variable:counter]} and the root message is store in
 * {@code #[mel:flowVars.rootMessage]}. Both variables may be renamed by means of {@link #setCounterVariableName(String)} and
 * {@link #setRootMessageVariableName(String)}.
 * <p>
 * Defining a groupSize greater than one, allows iterating over collections of elements of the specified size.
 * <p>
 * The {@link Event} sent to the next message processor is the same that arrived to foreach.
 */
public class Foreach extends AbstractMessageProcessorOwner implements Initialisable, Scope {

  public static final String ROOT_MESSAGE_PROPERTY = "rootMessage";

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private List<Processor> messageProcessors;
  private MessageProcessorChain ownedMessageProcessor;
  private AbstractMessageSequenceSplitter splitter;
  private String collectionExpression = DEFAULT_SPIT_EXPRESSION;
  private int batchSize;
  private String rootMessageVariableName;
  private String counterVariableName;
  private boolean xpathCollection;
  private String ignoreErrorType = null;

  @Override
  public Event process(Event event) throws MuleException {
    String parentMessageProp = rootMessageVariableName != null ? rootMessageVariableName : ROOT_MESSAGE_PROPERTY;
    Object previousCounterVar = null;
    Object previousRootMessageVar = null;
    if (event.getVariables().containsKey(counterVariableName)) {
      previousCounterVar = event.getVariables().get(counterVariableName).getValue();
    }
    if (event.getVariables().containsKey(parentMessageProp)) {
      previousRootMessageVar = event.getVariables().get(parentMessageProp).getValue();
    }
    Message message = event.getMessage();
    final Builder requestBuilder = Event.builder(event);
    boolean transformed = false;
    if (xpathCollection) {
      Message transformedMessage = transformPayloadIfNeeded(message);
      if (transformedMessage != message) {
        transformed = true;
        message = transformedMessage;
        requestBuilder.message(transformedMessage);
      }
    }
    requestBuilder.addVariable(parentMessageProp, message);
    final Builder responseBuilder = Event.builder(doProcess(requestBuilder.build()));
    if (transformed) {
      responseBuilder.message(transformBack(message));
    } else {
      responseBuilder.message(message);
    }
    if (previousCounterVar != null) {
      responseBuilder.addVariable(counterVariableName, previousCounterVar);
    } else {
      responseBuilder.removeVariable(counterVariableName);
    }
    if (previousRootMessageVar != null) {
      responseBuilder.addVariable(parentMessageProp, previousRootMessageVar);
    } else {
      responseBuilder.removeVariable(parentMessageProp);
    }
    return responseBuilder.build();
  }

  protected Event doProcess(Event event) throws MuleException {
    try {
      // TODO MULE-13052 Migrate Splitter and Foreach implementation to non-blocking
      return Mono.just(event)
          .then(request -> Mono
              .from(processWithChildContext(request, ownedMessageProcessor, ofNullable(getLocation()))))
          .onErrorMap(MessagingException.class, e -> {
            if (splitter.equals(e.getFailingMessageProcessor())) {
              // Make sure the context information for the exception is relative to the ForEach.
              e.getInfo().remove(INFO_LOCATION_KEY);
              return new MessagingException(event, e.getCause(), this);
            } else {
              return e;
            }
          }).block();
    } catch (Throwable throwable) {
      throw rxExceptionToMuleException(throwable);
    }
  }

  private Message transformPayloadIfNeeded(Message message) throws TransformerException {
    Object payload = message.getPayload().getValue();
    if (payload instanceof Document || payload.getClass().getName().startsWith("org.dom4j.")) {
      return message;
    } else {
      return muleContext.getTransformationService().internalTransform(message, DataType.fromType(Document.class));
    }
  }

  private Message transformBack(Message message) throws TransformerException {
    return muleContext.getTransformationService().internalTransform(message, DataType.STRING);
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(ownedMessageProcessor);
  }

  public void setMessageProcessors(List<Processor> messageProcessors) throws MuleException {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public void initialise() throws InitialisationException {
    ExpressionConfig expressionConfig = new ExpressionConfig();
    expressionConfig.setExpression(collectionExpression);
    splitter = new Splitter(expressionConfig, ignoreErrorType) {

      @Override
      protected void propagateFlowVars(Event previousResult, final Builder builder) {
        for (String flowVarName : resolvePropagatedFlowVars(previousResult).keySet()) {
          builder.addVariable(flowVarName, previousResult.getVariables().get(flowVarName).getValue(),
                              previousResult.getVariables().get(flowVarName).getDataType());
        }
      }

      @Override
      protected Map<String, TypedValue<?>> resolvePropagatedFlowVars(Event previousResult) {
        return previousResult != null ? previousResult.getVariables() : emptyMap();
      }

    };
    if (isXPathExpression(expressionConfig.getExpression())) {
      xpathCollection = true;
    }
    splitter.setBatchSize(batchSize);
    splitter.setCounterVariableName(counterVariableName);
    splitter.setMuleContext(muleContext);

    List<Processor> chainProcessors = new ArrayList<>();
    chainProcessors.add(splitter);
    chainProcessors.add(newChain(messageProcessors));
    ownedMessageProcessor = newChain(chainProcessors);

    super.initialise();
  }

  private boolean isXPathExpression(String expression) {
    return expression.matches("^xpath\\(.+\\)$") || expression.matches("^xpath3\\(.+\\)$");
  }

  public void setCollectionExpression(String expression) {
    this.collectionExpression = expression;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void setRootMessageVariableName(String rootMessageVariableName) {
    this.rootMessageVariableName = rootMessageVariableName;
  }

  public void setCounterVariableName(String counterVariableName) {
    this.counterVariableName = counterVariableName;
  }

  /**
   * Handles the given error types so that items that cause them when being processed are ignored, rather than propagating the
   * error.
   * <p>
   * This is useful to use validations inside this component.
   * 
   * @param ignoreErrorType A comma separated list of error types that should be ignored when processing an item.
   */
  public void setIgnoreErrorType(String ignoreErrorType) {
    this.ignoreErrorType = ignoreErrorType;
  }
}
