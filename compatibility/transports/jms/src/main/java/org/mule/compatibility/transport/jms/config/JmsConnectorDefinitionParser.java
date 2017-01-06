/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.config;

import org.mule.compatibility.transport.jms.JmsConnector;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;

import javax.jms.Session;

public class JmsConnectorDefinitionParser extends MuleOrphanDefinitionParser {

  public JmsConnectorDefinitionParser() {
    this(JmsConnector.class);
  }

  public JmsConnectorDefinitionParser(Class clazz) {
    super(clazz, true);
    addMapping("acknowledgementMode", "AUTO_ACKNOWLEDGE=" + Session.AUTO_ACKNOWLEDGE + "," + "CLIENT_ACKNOWLEDGE="
        + Session.CLIENT_ACKNOWLEDGE + "," + "DUPS_OK_ACKNOWLEDGE=" + Session.DUPS_OK_ACKNOWLEDGE);
  }

}
