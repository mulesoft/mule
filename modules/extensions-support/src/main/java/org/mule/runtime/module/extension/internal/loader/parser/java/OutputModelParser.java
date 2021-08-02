/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclarer;

public interface OutputModelParser {

  MetadataType getType();

  boolean isDynamic();

  default <T extends OutputDeclarer> OutputDeclarer<T> applyOn(OutputDeclarer<T> declarer) {
    if (isDynamic()) {
      declarer.ofDynamicType(getType());
    } else {
      declarer.ofType(getType());
    }

    return declarer;
  }
}
