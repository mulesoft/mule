/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;

@MediaType(TEXT_PLAIN)
public class SourceWithRequiredParameterWithAlias extends AbstractSource {

  //  @ParameterGroup(name = "someGroup")
  //  WithRequiredParameterWithAliasGroup group;
}
