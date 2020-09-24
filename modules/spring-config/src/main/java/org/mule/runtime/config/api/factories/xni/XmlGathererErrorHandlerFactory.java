/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.factories.xni;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.config.api.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.config.internal.factories.xni.DefaultXmlGathererErrorHandlerFactory;

/**
 * Factory object to create instances of {@link XmlGathererErrorHandler} that will be used in the reading of XML files.
 *
 * @since 4.4.0
 */
@NoImplement
public interface XmlGathererErrorHandlerFactory {

  public static XmlGathererErrorHandlerFactory getDefault() {
    return new DefaultXmlGathererErrorHandlerFactory();
  }

  public XmlGathererErrorHandler create();
}
