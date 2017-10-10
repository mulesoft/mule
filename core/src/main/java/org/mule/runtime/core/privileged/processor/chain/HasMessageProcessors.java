/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
