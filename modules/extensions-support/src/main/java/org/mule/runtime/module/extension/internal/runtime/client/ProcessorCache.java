/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.String.format;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Cache of {@link OperationMessageProcessor} to avoid initialization when used by the {@link ExtensionsClient}.
 *
 * @since 4.2.0
 */
public class ProcessorCache {

  private static final int MAX_CACHE_SIZE = 100;
  private static volatile ProcessorCache instance;

  private static String idFormat = "Extension:%sOperation:%sConfiguration:%s";

  private Cache<String, OperationMessageProcessor> cache = newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  private ProcessorCache() {
    if (instance != null) {
      throw new RuntimeException("Use getInstance() method to create and get a ProcessorCache");
    }
  }

  public static ProcessorCache getInstance() {
    if (instance == null) {
      synchronized (ProcessorCache.class) {
        if (instance == null) {
          instance = new ProcessorCache();
        }
      }
      return instance;
    }

    return instance;
  }

  public OperationMessageProcessor getOperationMessageProcessor(String extension, String operation,
                                                                OperationParameters parameters) {
    String operationMessageProcessorId = getOperationMessageProcessorId(extension, operation, parameters);
    return getInstance().cache.getIfPresent(operationMessageProcessorId);
  }

  public void putOperationMessageProcessor(String operationMessageProcessorId,
                                           OperationMessageProcessor operationMessageProcessor) {
    getInstance().cache.put(operationMessageProcessorId, operationMessageProcessor);
  }

  public String getOperationMessageProcessorId(String extension, String operation, OperationParameters parameters) {
    return format(idFormat, extension, operation, parameters.getConfigName().orElse(""));
  }

}
