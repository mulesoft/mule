/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
