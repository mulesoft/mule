/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.factories.xni;

import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.config.internal.factories.xni.DefaultXmlEntityResolverFactory;

/**
 * Factory object to create instances of {@link XMLEntityResolver} that will be used in the reading of XML files.
 *
 * @since 4.4.0
 */
@NoImplement
public interface XmlEntityResolverFactory {

  public static XmlEntityResolverFactory getDefault() {
    return new DefaultXmlEntityResolverFactory();
  }

  public XMLEntityResolver create();
}
