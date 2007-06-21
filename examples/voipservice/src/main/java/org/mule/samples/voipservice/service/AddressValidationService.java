/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.service;

import org.mule.samples.voipservice.interfaces.AddressValidation;
import org.mule.samples.voipservice.to.AddressTO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Binildas Christudas
 */
public class AddressValidationService implements AddressValidation
{

    protected static transient Log logger = LogFactory.getLog(AddressValidationService.class);

    public int validateAddress(AddressTO addressTO)
    {

        logger.info("Inside AddressValidationService.validateAddress() ***************");
        return AddressValidation.SUCCESS;
    }

}
