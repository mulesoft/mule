/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.sdk.api.runtime.source.Source;

public class SdkSourceAdapterFactory {

  public static Source createAdapter(Object source) {
    if (source instanceof Source) {
      return (Source) source;
    } else if (source instanceof org.mule.runtime.extension.api.runtime.source.Source) {
      return LegacySourceAdapter.from((org.mule.runtime.extension.api.runtime.source.Source) source);
    } else {
      throw new IllegalArgumentException("The given value needs to be a source!");
    }
  }

}
