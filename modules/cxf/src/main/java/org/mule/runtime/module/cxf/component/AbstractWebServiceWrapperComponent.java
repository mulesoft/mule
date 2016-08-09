/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.component;

import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.component.AbstractComponent;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWebServiceWrapperComponent extends AbstractComponent {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  public static final String WS_SERVICE_URL = "ws.service.url";

  protected String address;
  protected boolean addressFromMessage = false;

  protected void doInitialise() throws InitialisationException {
    if (address == null && !addressFromMessage) {
      throw new InitialisationException(CoreMessages.objectIsNull("webServiceUrl"), this);
    }
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isAddressFromMessage() {
    return addressFromMessage;
  }

  public void setAddressFromMessage(boolean addressFromMessage) {
    this.addressFromMessage = addressFromMessage;
  }

}
