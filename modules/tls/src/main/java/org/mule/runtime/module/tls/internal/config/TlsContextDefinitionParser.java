/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.config;

import org.mule.runtime.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;


public class TlsContextDefinitionParser extends ParentContextDefinitionParser {

  public TlsContextDefinitionParser() {
    super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(DefaultTlsContextFactory.class, true));
    and(MuleOrphanDefinitionParser.DOMAIN_ROOT_ELEMENT, new MuleOrphanDefinitionParser(DefaultTlsContextFactory.class, true));
    otherwise(new ChildDefinitionParser("tlsContext", DefaultTlsContextFactory.class));
  }

}
