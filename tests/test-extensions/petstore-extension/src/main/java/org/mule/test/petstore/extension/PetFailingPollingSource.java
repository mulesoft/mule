/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Alias("pet-failing-source")
@MediaType(TEXT_PLAIN)
public class PetFailingPollingSource extends PollingSource<String, Void> {

  public static final List<String> ALL_PETS =
      asList("Grumpy Cat", "Colonel Meow", "Skipped Cat", "Silvester", "Lil bub", "Macri", "Pappo");
  protected List<String> pets;
  private static Integer numberOfPolls = 0;
  public static ExecutorService executor;

  @Connection
  private ConnectionProvider<PetStoreClient> fileSystemProvider;

  @Parameter
  @Optional(defaultValue = "0")
  private Integer failAtPoll;

  @Parameter
  @Optional(defaultValue = "100")
  private Long adoptionLimit;

  @Override
  protected void doStart() throws MuleException {
    pets = new ArrayList<>(ALL_PETS);
  }

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    numberOfPolls++;
    if (numberOfPolls == failAtPoll) {
      //pollContext.onConnectionException(new ConnectionException("Polling Fail"));
      executor = newSingleThreadExecutor();
      executor.execute(() -> pollContext.onConnectionException(new ConnectionException("Polling Fail")));
    } else if (numberOfPolls - 1 <= adoptionLimit) {
      pollContext.accept(item -> {
        String pet = ALL_PETS.get((numberOfPolls - 1) % 7);
        item.setResult(Result.<String, Void>builder().output(pet).build());
      });
    }
  }

  @Override
  public void onRejectedItem(Result result, SourceCallbackContext callbackContext) {

  }
}
