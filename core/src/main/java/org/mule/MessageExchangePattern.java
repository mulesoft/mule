/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

public enum MessageExchangePattern
{    
    ONE_WAY
    {
        @Override
        public boolean hasResponse()
        {
            return false;
        }
    }, 
    
    REQUEST_RESPONSE
    {
        @Override
        public boolean hasResponse()
        {
            return true;
        }
    }; 
    
    public abstract boolean hasResponse();

    public static MessageExchangePattern fromSyncFlag(boolean sync)
    {
        if (sync)
        {
            return REQUEST_RESPONSE;
        }
        else
        {
            return ONE_WAY;
        }
    }

    public static MessageExchangePattern fromString(String string)
    {
        String mepString = string.toUpperCase().replace('-', '_');
        return MessageExchangePattern.valueOf(mepString);
    }
}
