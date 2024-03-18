/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Comparator.naturalOrder;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class ConnectedPetAdoptionSource extends PetAdoptionSource {

  @Connection
  private ConnectionProvider<PetStoreClient> fileSystemProvider;

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    pollContext.setWatermarkComparator(naturalOrder());
    pets.stream()
        .map(p -> Result.<String, Void>builder().output(p).build())
        .forEach(result -> pollContext.accept(item -> {
          try {
            item.getSourceCallbackContext().bindConnection(fileSystemProvider.connect());
          } catch (Exception e) {
            // You will never get here
          }
          item.setResult(result);

          if (idempotent) {
            item.setId(result.getOutput());
          }

          if (watermark) {
            item.setWatermark(pets.indexOf(result.getOutput()));
          }
        }));
  }
}
