/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;

public class EventDropperProcessor extends AbstractComponent implements Processor {

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> p) {
    Hooks.onNextDropped(event -> {
      System.out.println("Event dropped: " + event);
    });

    Hooks.onErrorDropped(error -> {
      System.out.println("Error dropped: " + error.getMessage());
    });

    return Flux.from(p)
        .<CoreEvent>map(event -> {
          throw new RuntimeException("Forced error in EventDropperProcessor");
        })
        .onErrorContinue((error, obj) -> {
          // This will trigger the error dropped hook since we're not handling the error
          throw new RuntimeException("Secondary error: " + error.getMessage());
        });
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return event;
  }

}
