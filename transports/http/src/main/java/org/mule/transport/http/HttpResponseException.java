/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

/**
 * A wrapper exceptin for any http client return codes over the 400 range
 */
public class HttpResponseException extends Exception
{
    private String responseText;
    private int responseCode;

    public HttpResponseException(String responseText, int responseCode)
    {
        super(responseText + ", code: " + responseCode);
        this.responseCode = responseCode;
        this.responseText = responseText;
    }

    public String getResponseText()
    {
        return responseText;
    }

    public int getResponseCode()
    {
        return responseCode;
    }
}
