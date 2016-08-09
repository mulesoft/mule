/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.execution.BlockingCompletionHandler;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceContext;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Alias("ListenPayments")
public class HeisenbergSource extends Source<Void, Attributes> implements Initialisable {

  public static final String CORE_POOL_SIZE_ERROR_MESSAGE = "corePoolSize cannot be a negative value";
  public static final String INITIAL_BATCH_NUMBER_ERROR_MESSAGE = "initialBatchNumber cannot be a negative value";

  private ScheduledExecutorService executor;

  @UseConfig
  private HeisenbergExtension heisenberg;

  @Connection
  private HeisenbergConnection connection;

  @Parameter
  private volatile int initialBatchNumber;

  @Parameter
  @Optional(defaultValue = "1")
  private int corePoolSize;

  @Override
  public void initialise() throws InitialisationException {
    checkArgument(heisenberg != null, "config not injected");
    connection.verifyLifecycle(1, 1, 0, 0);
  }

  @Override
  public void start() {
    connection.verifyLifecycle(1, 1, 0, 0);
    HeisenbergExtension.sourceTimesStarted++;

    if (corePoolSize < 0) {
      throw new RuntimeException(CORE_POOL_SIZE_ERROR_MESSAGE);
    }

    executor = newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(() -> sourceContext.getMessageHandler().handle(makeMessage(sourceContext), completionHandler()),
                                 0, 100, TimeUnit.MILLISECONDS);
  }

  private CompletionHandler<MuleEvent, Exception, MuleEvent> completionHandler() {
    return new BlockingCompletionHandler<MuleEvent, Exception, MuleEvent>() {

      @Override
      protected void doOnCompletion(MuleEvent event) {
        Long payment = (Long) ((org.mule.runtime.core.api.MuleEvent) event).getMessage().getPayload();
        heisenberg.setMoney(heisenberg.getMoney().add(BigDecimal.valueOf(payment)));
      }

      @Override
      public void onFailure(Exception exception) {
        heisenberg.setMoney(BigDecimal.valueOf(-1));
      }
    };
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
      try {
        executor.awaitTermination(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private MuleMessage makeMessage(SourceContext sourceContext) {
    if (initialBatchNumber < 0) {
      sourceContext.getExceptionCallback().onException(new RuntimeException(INITIAL_BATCH_NUMBER_ERROR_MESSAGE));
    }

    return MuleMessage.builder()
        .payload(format("Meth Batch %d. If found by DEA contact %s", ++initialBatchNumber, connection.getSaulPhoneNumber()))
        .build();
  }


}
