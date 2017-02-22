/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.api.processor.Processor;

/**
 * Marker interface for MessageProcessors that actually represent a subflow.
 */
public interface SubFlowMessageProcessor extends Processor {

  String getSubFlowName();

}
