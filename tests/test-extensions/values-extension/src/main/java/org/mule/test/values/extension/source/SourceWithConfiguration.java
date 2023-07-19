/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.config.SimpleConfig;
import org.mule.test.values.extension.resolver.WithConfigValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithConfiguration extends AbstractSource {

  @OfValues(WithConfigValueProvider.class)
  @Parameter
  String channel;

  @org.mule.sdk.api.annotation.param.Config
  SimpleConfig simpleConfig;

}
