/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
