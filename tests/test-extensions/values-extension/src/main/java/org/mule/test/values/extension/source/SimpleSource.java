/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.source;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

public class SimpleSource extends AbstractSource {

  @OfValues(SimpleValueProvider.class)
  @Parameter
  String channel;

}
