/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.component;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.component.AbstractComponent;
import org.mule.config.i18n.CoreMessages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractWebServiceWrapperComponent extends AbstractComponent
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final String WS_SERVICE_URL = "ws.service.url";

    protected String address;
    protected boolean addressFromMessage = false;

    protected void doInitialise() throws InitialisationException
    {
        if (address == null && !addressFromMessage)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("webServiceUrl"), this);
        }
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public boolean isAddressFromMessage()
    {
        return addressFromMessage;
    }

    public void setAddressFromMessage(boolean addressFromMessage)
    {
        this.addressFromMessage = addressFromMessage;
    }

}
