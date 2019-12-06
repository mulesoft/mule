/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.some.extension;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;

import org.slf4j.Logger;

/**
 * Some operations
 */
public class SomeOps {

  private final Logger LOGGER = getLogger(SomeOps.class);

  public void someOp(@Connection String conn, @Config SomeExtension ext) {}

  /**
   * An operation to test the ByteArray to InputStream value transformation.
  *
   * @param value the value for the operation to consume
   * @return a byte array representation of <it>value</it>
   */
  public Result<InputStream, Object> inputStreamConsumingOperation(@Content TypedValue<InputStream> value) {
    LOGGER.info("A new message is passing through 'inputStreamConsumingOperation': {}", value.getValue());
    return Result.<InputStream, Object>builder().output(value.getValue()).attributes(null).build();
  }

  /**
   * An operation to test an use-case of a ParameterGroup shown as a child-element of the operation xml definition, with a
   * <it>isOneRequired</it> property.
   *
   * @param oneParameterGroup some test operation config with a isOneRequired exclusive-optional configuration
   * @return one of the configs arguments
   */
  public String oneRequiredParameterResolverOperation(@ParameterGroup(
      name = "Awesome Parameter Group", showInDsl = true) SomeParameterGroupOneRequiredConfig oneParameterGroup) {
    return oneParameterGroup.getSomeParameter() != null ? oneParameterGroup.getSomeParameter()
        : oneParameterGroup.getSomeOtherParameter();
  }
}
