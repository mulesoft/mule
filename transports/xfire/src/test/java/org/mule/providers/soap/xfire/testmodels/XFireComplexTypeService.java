/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.testmodels;

import org.mule.tck.testmodels.services.Person;
import org.mule.tck.testmodels.services.PersonResponse;

public class XFireComplexTypeService
{

    public PersonResponse addPersonWithConfirmation(Person person)
    {
        return new PersonResponse(person);
    }
}
