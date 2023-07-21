/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.WithErrorValueProvider;

@Alias("failure-connection")
public class ConnectionWithFailureErrorProvider extends AbstractConnectionProvider {

  @Parameter
  @OfValues(WithErrorValueProvider.class)
  String values;

  @Parameter
  String errorCode;

}
