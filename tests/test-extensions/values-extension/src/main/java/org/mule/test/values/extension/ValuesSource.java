/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.Map;


public class ValuesSource extends Source<Map<String, String>, Attributes> {

  @MetadataKeyId
  @Parameter
  public String type;

  @Connection
  private ValuesConnection connection;

  @Override
  public void onStart(SourceCallback<Map<String, String>, Attributes> sourceCallback) throws MuleException {}

  @Override
  public void onStop() {

  }
}
