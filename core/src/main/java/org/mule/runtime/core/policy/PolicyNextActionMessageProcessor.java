/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Next-operation message processor implementation.
 *
 * Such implementation handles a set of callbacks to execute as next operations that are must be configured before processing the
 * event.
 *
 * @since 4.0
 */
public class PolicyNextActionMessageProcessor implements Processor {

  @Inject
  private PolicyStateHandler policyStateHandler;

  @Override
  public Event process(Event event) throws MuleException {
    policyStateHandler.updateState(event.getContext().getId(), event);
    NextOperation nextOperation = policyStateHandler.retrieveNextOperation(event.getContext().getId());
    if (nextOperation == null) {
      throw new MuleRuntimeException(createStaticMessage("There's no next operation configured for event context id "
          + event.getContext().getId()));
    }
    try {
      Event result = nextOperation.execute(Event.builder(event.getContext()).message(event.getMessage()).build());
      Event.Builder eventBuilder = Event.builder(result.getContext()).message(result.getMessage());
      for (String variableName : event.getVariableNames()) {
        eventBuilder.addVariable(variableName, event.getVariable(variableName).getValue(),
                                 event.getVariable(variableName).getDataType());
      }
      return eventBuilder.build();
    } catch (MessagingException e) {
      throw e;
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

}
