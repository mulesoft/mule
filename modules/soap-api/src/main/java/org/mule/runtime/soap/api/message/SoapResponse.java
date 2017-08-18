/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.extension.api.soap.SoapAttributes;
import org.mule.runtime.extension.api.soap.SoapOutputPayload;

/**
 * Represents a response retrieved by a Soap Web Service.
 *
 * @since 4.0
 */
public interface SoapResponse extends SoapMessage {

  Result<SoapOutputPayload, SoapAttributes> getAsResult(StreamingHelper helper);
}
