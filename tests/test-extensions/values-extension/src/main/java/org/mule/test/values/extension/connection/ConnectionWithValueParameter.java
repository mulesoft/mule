/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

@Alias("with-value-parameter")
public class ConnectionWithValueParameter extends AbstractConnectionProvider {

  @Parameter
  @OfValues(SimpleValueProvider.class)
  String channel;

}
