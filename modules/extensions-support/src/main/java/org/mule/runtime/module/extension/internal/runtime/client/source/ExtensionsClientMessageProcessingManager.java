/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.execution.FlowProcessTemplate;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.core.internal.execution.MessageProcessingManager;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionsFlowProcessingTemplate;

import java.util.function.Consumer;

/**
 * Implementation of {@link MessageProcessingManager} for sources created through the {@link ExtensionsClient}
 *
 * @since 4.6.0
 */
class ExtensionsClientMessageProcessingManager<T, A> implements MessageProcessingManager {

  private final SourceClient sourceClient;
  private final Consumer<SourceResultCallback<T, A>> callbackConsumer;

  ExtensionsClientMessageProcessingManager(SourceClient sourceClient,
                                           Consumer<SourceResultCallback<T, A>> callbackConsumer) {
    this.sourceClient = sourceClient;
    this.callbackConsumer = callbackConsumer;
  }

  @Override
  public void processMessage(FlowProcessTemplate template, MessageProcessContext messageProcessContext) {
    checkArgument(template instanceof ExtensionsFlowProcessingTemplate, "invalid processing template");
    ExtensionsFlowProcessingTemplate process = (ExtensionsFlowProcessingTemplate) template;
    SourceResultAdapter resultAdapter = process.getSourceMessage();

    Result result = getResult(resultAdapter);

    callbackConsumer.accept(new DefaultSourceResultCallback<>(sourceClient, result, process));
  }

  private Result<Object, Object> getResult(SourceResultAdapter adapter) {
    org.mule.sdk.api.runtime.operation.Result<Object, Object> sdkResult =
        adapter.getPayloadMediaTypeResolver().resolve(adapter.getResult());

    return withNullEvent(event -> {
      EventContext context = event.getContext();
      Result.Builder<Object, Object> builder = Result.builder()
          .output(adapter.getCursorProviderFactory().of(context, sdkResult.getOutput(), context.getOriginatingLocation()));

      if (sdkResult.getMediaType().isPresent()) {
        builder.mediaType(sdkResult.getMediaType().get());
      }
      sdkResult.getMediaType().ifPresent(builder::mediaType);
      sdkResult.getAttributes().ifPresent(builder::attributes);
      sdkResult.getAttributesMediaType().ifPresent(builder::attributesMediaType);
      sdkResult.getByteLength().ifPresent(builder::length);

      return builder.build();
    });
  }
}
