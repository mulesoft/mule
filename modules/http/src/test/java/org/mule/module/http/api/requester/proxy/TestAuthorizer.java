/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface TestAuthorizer
{
    /**
     * Method to implement the authorization test logic.
     * 
     * @param address target address
     * @param request HTTP request
     * @param response HTTP response
     * @param addAuthorizeMessageInProxy whether the authorization has to be sent from proxy
     * 
     * @return request authorized
     * @throws IOException excetpion in request
     */
    boolean authorizeRequest(String address, HttpServletRequest request, HttpServletResponse response, boolean addAuthorizeMessageInProxy) throws IOException;
}
