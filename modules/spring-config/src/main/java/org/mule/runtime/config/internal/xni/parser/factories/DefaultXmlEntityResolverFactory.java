/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser.factories;

import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import org.mule.runtime.config.internal.xni.parser.DefaultXmlEntityResolver;

/**
 *
 * @since 4.4.0
 */
public class DefaultXmlEntityResolverFactory implements XmlEntityResolverFactory {

  @Override
  public XMLEntityResolver create() {
    return new DefaultXmlEntityResolver();
  }
}
