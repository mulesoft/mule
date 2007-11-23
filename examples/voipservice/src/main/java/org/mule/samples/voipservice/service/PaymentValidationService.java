/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.service;

import org.mule.RegistryContext;
import org.mule.samples.voipservice.interfaces.PaymentValidation;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PaymentValidationService implements PaymentValidation
{

    protected static transient Log logger = LogFactory.getLog(PaymentValidationService.class);

    public List getCreditVendors(String cardType) throws UMOException
    {
        logger.info("Inside PaymentValidationService.getCreditVendors() ***************");
        List endPoints = new ArrayList();
        // we can use endpoint names...
        endPoints.add(CREDIT_AGENCY_LOOKUP_NAME);
        // ..or serialized URIs.
        endPoints.add(getEndpointUri(BANK_AGENCY_LOOKUP_NAME));
        return endPoints;
    }

    private UMOEndpointURI getEndpointUri(String endpointName) throws UMOException
    {
        UMOImmutableEndpoint endpoint = RegistryContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointName);
        return endpoint.getEndpointURI();
    }

}
