/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.List;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class PetAdoptionLimitingSource extends PollingSource<String, Integer> {

  private int pollCounter;

  @Parameter
  private List<String> pets;

  @Parameter
  private List<Integer> watermarks;

  @Override
  protected void doStart() throws MuleException {
    resetCounters();
  }

  @Override
  protected void doStop() {
    resetCounters();
  }

  @Override
  public void poll(PollContext<String, Integer> pollContext) {
    for (int i = 0; i < pets.size(); i++) {
      final int index = i;
      pollContext.accept(item -> {
        item.setResult(Result.<String, Integer>builder().output(pets.get(index)).attributes(pollCounter).build());
        item.setId(pets.get(index));
        item.setWatermark(watermarks.get(index));
      });
    }
    pollCounter++;
  }

  @Override
  public void onRejectedItem(Result<String, Integer> result, SourceCallbackContext callbackContext) {

  }

  private synchronized void resetCounters() {
    pollCounter = 0;
  }
}
