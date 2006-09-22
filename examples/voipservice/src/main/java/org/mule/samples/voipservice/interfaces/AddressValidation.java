/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.voipservice.interfaces;

import org.mule.samples.voipservice.to.AddressTO;

import java.io.IOException;

/**
 * @author Binildas Christudas
 */
public interface AddressValidation {

    int SUCCESS = 1;
    int FAILURE = -1;

    int validateAddress(AddressTO addressTO) throws IOException;
}