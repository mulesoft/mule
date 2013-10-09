/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.util.StringUtils;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A filter that will determine if the current message payload is a JSON encoded message.
 */
public class IsJsonFilter implements Filter
{

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(IsJsonFilter.class);

    private boolean validateParsing = false;

    public IsJsonFilter()
    {
        super();
    }

    public boolean accept(MuleMessage obj)
    {
        // TODO should be checking inbound IMO
        final String contentType = obj.getOutboundProperty("Content-Type", StringUtils.EMPTY);
        if (contentType.contains("application/json"))
        {
            return true;
        }
        try
        {
            return accept(obj.getPayloadAsString());
        }
        catch (Exception e)
        {
            logger.warn("Failed to read object payload as string for isJsonFilter", e);
            return false;
        }
    }

    public boolean accept(Object obj)
    {

        try
        {
            if (obj instanceof byte[])
            {
                obj = new String((byte[])obj);
            }

            if (obj instanceof String)
            {
                if (!mayBeJSON((String) obj))
                {
                    return false;
                }

                if (isValidateParsing())
                {
                    new ObjectMapper().readTree((String) obj);
                }
                return true;
            }
            else
            {
                return false;
            }

        }
        catch (IOException e)
        {
            logger.error("Filter result = false (message is not valid JSON): " + e.getMessage());
            return false;
        }
    }

    public boolean isValidateParsing()
    {
        return validateParsing;
    }

    public void setValidateParsing(boolean validateParsing)
    {
        this.validateParsing = validateParsing;
    }

    /**
     * Tests if the String possibly represents a valid JSON String.
     *
     * @param string Valid JSON strings are:
     *               <ul>
     *               <li>"null"</li>
     *               <li>starts with "[" and ends with "]"</li>
     *               <li>starts with "{" and ends with "}"</li>
     *               </ul>
     * @return true if the test string starts with one of the valid json characters
     */
    protected boolean mayBeJSON(String string)
    {
        return string != null
                && ("null".equals(string)
                || (string.startsWith("[") && string.endsWith("]")) || (string.startsWith("{") && string.endsWith("}")));
   }
}
