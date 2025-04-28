/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

/**
 * This is kept for backwards compatibility with persistent serialized data.
 *
 * @since 4.0
 */
public class DefaultFunctionDataType extends org.mule.runtime.core.privileged.metadata.DefaultFunctionDataType {

  private static final long serialVersionUID = 5452095230956829108L;

  public DefaultFunctionDataType() {
    super(null, null, null, null, false);
  }

}
