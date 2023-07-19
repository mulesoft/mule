/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 * Contract interface for a class which can return a list of {@link Processor}
 *
 * @since 4.0
 */
public interface HasMessageProcessors {

  List<Processor> getMessageProcessors();
}
