/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.message.StringAttributes;
import org.mule.tck.testmodels.fruit.Peach;

@Alias("harvest-peaches")
@MediaType(TEXT_PLAIN)
public class HarvestPeachesSource extends Source<String, StringAttributes> {

  public static boolean isConnected;

  @Connection
  private ConnectionProvider<Peach> connection;


  @Override
  public void onStart(SourceCallback<String, StringAttributes> sourceCallback) throws MuleException {
    isConnected = connection.connect() != null;
  }

  @Override
  public void onStop() {}
}
