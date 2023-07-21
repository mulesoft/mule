/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.ValuesConnection;
import org.mule.test.values.extension.resolver.WithConnectionValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithConnection extends AbstractSource {

  @OfValues(WithConnectionValueProvider.class)
  @Parameter
  String channel;

  @org.mule.sdk.api.annotation.param.Connection
  ConnectionProvider<ValuesConnection> connection;

}
