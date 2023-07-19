/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

@Alias("pet-filter-source")
@MediaType(TEXT_PLAIN)
public class PetFilterPollingSource extends PollingSource<String, Void> {

  private static final String TIGER = "tiger";
  private static final String WHALE = "whale";
  private static final String DINOSAUR = "dinosaur";

  @Parameter
  private String filter;

  @Override
  protected void doStart() {}

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    if (TIGER.equalsIgnoreCase(filter)) {
      throw new RuntimeException(new ConnectionException("A tiger cannot be petted."));
    }
    if (WHALE.equalsIgnoreCase(filter)) {
      throw new RuntimeException("Why do you want to pet a whale?");
    }
    if (DINOSAUR.equalsIgnoreCase(filter)) {
      pollContext.onConnectionException(new ConnectionException("Dinosaurs no longer exist."));
    }
  }

  @Override
  public void onRejectedItem(Result result, SourceCallbackContext callbackContext) {}
}
