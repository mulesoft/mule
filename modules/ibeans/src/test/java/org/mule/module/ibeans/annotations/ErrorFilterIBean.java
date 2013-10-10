/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;


import org.ibeans.annotation.Template;
import org.ibeans.annotation.filter.ExpressionErrorFilter;
import org.ibeans.annotation.filter.JsonErrorFilter;
import org.ibeans.annotation.filter.XmlErrorFilter;
import org.ibeans.annotation.param.Body;
import org.ibeans.annotation.param.HeaderParam;
import org.ibeans.api.CallException;

/**
 * TODO
 */
@JsonErrorFilter(expr = "/message/errorMsg", errorCode = "/message/error")
@XmlErrorFilter(expr = "/message/errorMsg", errorCode = "/message/error")
@ExpressionErrorFilter(eval = "regex", expr = "errorMsg", mimeType = "text/plain")
public interface ErrorFilterIBean
{
    @Template("")
    public String jsonErrorFilter(@Body String testData, @HeaderParam("Content-Type") String contentType) throws CallException;

    @Template("")
    public String xmlErrorFilter(@Body String testData, @HeaderParam("Content-Type") String contentType) throws CallException;

    @Template("")
    public String regExErrorFilter(@Body String testData, @HeaderParam("Content-Type") String contentType) throws CallException;
}
