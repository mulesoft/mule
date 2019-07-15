/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Alias("pet-failing-source")
public class PetFailingPollingSource extends PollingSource<String, Instant> {

  public static final List<String> ALL_PETS =
      asList("Grumpy Cat", "Colonel Meow", "Skipped Cat", "Silvester", "Lil bub", "Macri", "Pappo");
  protected List<String> pets;
  private static Integer numberOfPolls = 0;
  public static ExecutorService executor;

  @Connection
  private ConnectionProvider<PetStoreClient> fileSystemProvider;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "0")
  private Long sleepAtRestart;

  @Override
  protected void doStart() throws MuleException {
    pets = new ArrayList<>(ALL_PETS);
  }

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Instant> pollContext) {
    numberOfPolls++;
    if (numberOfPolls == 3) {
      if (sleepAtRestart != 0) {
        try {
          Thread.sleep(sleepAtRestart);
        } catch (InterruptedException e) {
          // No op
        }
      }
      //pollContext.onConnectionException(new ConnectionException("Polling Fail"));
      executor = newSingleThreadExecutor();
      executor.execute(() -> pollContext.onConnectionException(new ConnectionException("Polling Fail")));
    } else if (numberOfPolls <= 7) {
      pollContext.accept(item -> {
        String pet = ALL_PETS.get((numberOfPolls - 1));
        Instant instant = Instant.now();
        item.setResult(Result.<String, Instant>builder().attributes(instant).output(pet).build());
      });
    }
  }

  @Override
  public void onRejectedItem(Result result, SourceCallbackContext callbackContext) {

  }
}
