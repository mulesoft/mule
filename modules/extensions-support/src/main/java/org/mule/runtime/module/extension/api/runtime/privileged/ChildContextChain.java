/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Chain;

import javax.xml.namespace.QName;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Extension of @{link Chain} that adds the feature to execute the chain with a child context
 * ({@link org.mule.runtime.core.internal.event.DefaultEventContext#child})
 *
 * @since 4.4.0
 */
public interface ChildContextChain extends Chain {

  /**
   * Key for annotation to be used to set the location of the owner of this {@link ChildContextChain}.
   *
   * @since 4.7
   */
  QName CHAIN_OWNER_LOCATION_KEY = new QName("http://www.mulesoft.org/schema/mule/parser-metadata", "CHAIN_OWNER_LOCATION");

  /**
   * Same as {@link Chain#process(Consumer, BiConsumer)}, setting the correlation id within the execution as the the one passed.
   * <p>
   * The correlation id will return to the previous values for the components following the chain execution.
   *
   * @param correlationId the correlation id to be used in the event within the execution of the chain
   * @param onSuccess     the callback to be executed when a successful execution is completed by the {@link Chain}
   * @param onError       the callback to be executed when an error occurs during the execution of the {@link Chain} components
   */
  void process(String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError);

}
