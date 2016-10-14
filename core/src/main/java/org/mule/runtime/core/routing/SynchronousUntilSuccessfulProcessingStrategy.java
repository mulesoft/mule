/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.api.Event.setCurrentEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.session.DefaultMuleSession;

import java.io.NotSerializableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Until successful synchronous processing strategy. It will execute the until-successful router within the callers thread.
 */
public class SynchronousUntilSuccessfulProcessingStrategy extends AbstractUntilSuccessfulProcessingStrategy
    implements Initialisable {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  protected Event doRoute(Event event, FlowConstruct flow) throws MuleException {
    Exception lastExecutionException = null;
    Event retryEvent = copyEventForRetry(event);
    try {
      for (int i = 0; i <= getUntilSuccessfulConfiguration().getMaxRetries(); i++) {
        try {
          Event successEvent = processResponseThroughAckResponseExpression(processEvent(retryEvent));
          if (successEvent == null) {
            return null;
          }
          Event finalEvent;
          Builder builder = Event.builder(event).message(successEvent.getMessage());
          for (String flowVar : successEvent.getVariableNames()) {
            builder.addVariable(flowVar, successEvent.getVariable(flowVar).getValue());
          }
          finalEvent = builder.build();
          setCurrentEvent(finalEvent);
          return finalEvent;
        } catch (Exception e) {
          logger.info("Exception thrown inside until-successful " + e.getMessage());
          if (logger.isDebugEnabled()) {
            logger.debug("Exception thrown inside until-successful ", e);
          }
          lastExecutionException = e;
          if (i < getUntilSuccessfulConfiguration().getMaxRetries()) {
            Thread.sleep(getUntilSuccessfulConfiguration().getMillisBetweenRetries());
            retryEvent = copyEventForRetry(event);
          }
        }
      }
      throw new RoutingException(getUntilSuccessfulConfiguration().getRouter(), lastExecutionException);
    } catch (Exception e) {
      throw new RoutingException(getUntilSuccessfulConfiguration().getRouter(), e);
    }
  }

  private Event copyEventForRetry(Event event) {
    Event copy = Event.builder(event).session(new DefaultMuleSession(event.getSession())).build();
    setCurrentEvent(copy);
    return copy;
  }


  @Override
  public void initialise() throws InitialisationException {
    if (getUntilSuccessfulConfiguration().getThreadingProfile() != null) {
      throw new InitialisationException(CoreMessages
          .createStaticMessage("Until successful cannot be configured to be synchronous and have a threading profile at the same time"),
                                        this);
    }
    if (getUntilSuccessfulConfiguration().getObjectStore() != null) {
      throw new InitialisationException(CoreMessages
          .createStaticMessage("Until successful cannot be configured to be synchronous and use an object store."), this);
    }
    if (getUntilSuccessfulConfiguration().getDlqMP() != null) {
      throw new InitialisationException(CoreMessages
          .createStaticMessage("Until successful cannot be configured to be synchronous and use a dead letter queue. Failure must be processed with exception strategy"),
                                        this);
    }
  }

  @Override
  protected void ensureSerializable(InternalMessage message) throws NotSerializableException {
    // Message is not required to be Serializable because it is kept in memory
  }

}
