/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.services;

/**
 * <code>PeopleService</code> is a test service that returns complex types
 */

public interface PeopleService
{
    public Person getPerson(String firstName);

    public Person[] getPeople();

    public void addPerson(Person person) throws Exception;

    public Person addPerson(String firstname, String surname) throws Exception;
}
