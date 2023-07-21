/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
