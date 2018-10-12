/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils to manage request resources
 * 
 * @since 3.9.10
 *
 */
public class RequestResourcesUtils
{
    private static final Logger logger = LoggerFactory.getLogger(RequestResourcesUtils.class);

    public static void closeResources(HttpRequest request)
    {
        if (request.getEntity() instanceof InputStreamHttpEntity)
        {
            try
            {
                ((InputStreamHttpEntity) request.getEntity()).getInputStream().close();
            }
            catch (Exception e)
            {
                logger.warn("Error on closing the input stream of a grizzly request: {}", e.getMessage());
            }
        }
    }
}
