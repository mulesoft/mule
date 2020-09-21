/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser.factories;

import org.mule.runtime.config.internal.xni.parser.XmlGathererErrorHandler;

/**
 *
 * @since 4.4.0
 */
public interface XmlGathererErrorHandlerFactory {

  public static XmlGathererErrorHandlerFactory getDefault() {
    return new DefaultXmlGathererErrorHandlerFactory();
  }

  public XmlGathererErrorHandler create();
}
