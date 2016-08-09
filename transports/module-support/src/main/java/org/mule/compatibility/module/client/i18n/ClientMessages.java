/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client.i18n;

import org.mule.runtime.core.config.i18n.MessageFactory;

public class ClientMessages extends MessageFactory {

  private static final ClientMessages factory = new ClientMessages();

  private static final String BUNDLE_PATH = getBundlePath("client");

}


