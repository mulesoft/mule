/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import org.mule.runtime.core.MessageExchangePattern;

public class NonBlockingFullySupportedOneWayReplyToEndpointFunctionalTestCase
    extends NonBlockingFullySupportedEndpointFunctionalTestCase {

  @Override
  protected MessageExchangePattern getMessageExchnagePattern() {
    return MessageExchangePattern.ONE_WAY;
  }
}
