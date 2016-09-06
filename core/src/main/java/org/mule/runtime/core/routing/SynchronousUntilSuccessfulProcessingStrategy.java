/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
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
  protected MuleEvent doRoute(MuleEvent event, FlowConstruct flow) throws MuleException {
    Exception lastExecutionException = null;
    MuleEvent retryEvent = copyEventForRetry(event);
    try {
      for (int i = 0; i <= getUntilSuccessfulConfiguration().getMaxRetries(); i++) {
        try {
          MuleEvent successEvent = processResponseThroughAckResponseExpression(processEvent(retryEvent));
          MuleEvent finalEvent;
          if (successEvent instanceof VoidMuleEvent) {
            // continue processing with the original event
            finalEvent = event;
          } else {
            Builder builder = MuleEvent.builder(event).message(successEvent.getMessage());
            for (String flowVar : successEvent.getFlowVariableNames()) {
              builder.addFlowVariable(flowVar, successEvent.getFlowVariable(flowVar));
            }
            finalEvent = builder.build();
          }
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

  private MuleEvent copyEventForRetry(MuleEvent event) {
    MuleEvent copy = MuleEvent.builder(event).session(new DefaultMuleSession(event.getSession())).build();
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
  protected void ensureSerializable(MuleMessage message) throws NotSerializableException {
    // Message is not required to be Serializable because it is kept in memory
  }

}
