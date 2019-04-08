/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static java.lang.String.format;
import static java.util.regex.Matcher.quoteReplacement;
import static java.util.regex.Pattern.compile;
import org.mule.api.MuleEvent;
import org.mule.module.http.internal.HttpMessageBuilder;
import org.mule.module.http.internal.HttpParamType;
import org.mule.module.http.internal.ParameterMap;

import java.util.List;
import java.util.regex.Pattern;

public class HttpRequesterRequestBuilder extends HttpMessageBuilder
{

    public ParameterMap getQueryParams(MuleEvent event)
    {
        return resolveParams(event, HttpParamType.QUERY_PARAM);
    }

    public ParameterMap getHeaders(MuleEvent event)
    {
        return resolveParams(event, HttpParamType.HEADER);
    }

    public String replaceUriParams(String path, MuleEvent event)
    {
        ParameterMap uriParamMap = resolveParams(event, HttpParamType.URI_PARAM);

        for (String uriParamName : uriParamMap.keySet())
        {
            // If more than one value has been defined for a URI param, use the last one (to allow overrides)

            List<String> uriParamValues = uriParamMap.getAll(uriParamName);
            String uriParamValue = uriParamValues.get(uriParamValues.size() - 1);

            if (uriParamValue == null)
            {
                throw new NullPointerException(format("Expression {%s} evaluated to null.", uriParamName));
            }

            String value = quoteReplacement(uriParamValue);

            Pattern pattern = compile("\\{" + uriParamName + "}");

            path = pattern.matcher(path).replaceAll(value);
        }
        return path;
    }

}
