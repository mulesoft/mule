/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
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