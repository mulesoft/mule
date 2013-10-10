/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
