/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
