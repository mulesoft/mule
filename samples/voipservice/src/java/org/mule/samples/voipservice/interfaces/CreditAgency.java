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

import org.mule.samples.voipservice.to.CreditProfileTO;

import java.io.IOException;

/**
 * @author Binildas Christudas
 */
public interface CreditAgency {

    CreditProfileTO getCreditProfile(CreditProfileTO creditProfileTO) throws IOException;
}