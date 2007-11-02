/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.interfaces;

import org.mule.umo.UMOException;

import java.io.IOException;
import java.util.List;

public interface PaymentValidation
{

    int SUCCESS = 1;
    int FAILURE = -1;
    String CREDIT_AGENCY_LOOKUP_NAME = "CreditAgency";
    String BANK_AGENCY_LOOKUP_NAME = "BankAgency";

    List getCreditVendors(String cardType) throws IOException, UMOException;

}
