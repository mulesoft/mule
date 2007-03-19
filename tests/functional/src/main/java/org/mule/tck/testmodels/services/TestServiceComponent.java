/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.services;

import org.mule.components.simple.EchoService;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.lifecycle.Disposable;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * <code>TestServiceComponent</code> is a test WebServices component.
 */
public class TestServiceComponent extends FunctionalTestComponent
    implements EchoService, DateService, PeopleService, Disposable
{
    // we keep two collections - one static for testing the return of complex types
    // and one for modifying by methods invoked on the TestComponent instance

    private static final Person[] originalPeople = new Person[]{new Person("Barney", "Rubble"),
        new Person("Fred", "Flintstone"), new Person("Wilma", "Flintstone")};

    private final Map people = Collections.synchronizedMap(new HashMap());

    public TestServiceComponent()
    {
        super();
        people.put("Barney", originalPeople[0]);
        people.put("Fred", originalPeople[1]);
        people.put("Wilma", originalPeople[2]);
    }

    public String echo(String echo)
    {
        return echo;
    }

    public String getDate()
    {
        return new Date().toString();
    }

    public Person getPerson(String firstName)
    {
        if (StringUtils.isEmpty(firstName))
        {
            throw new IllegalArgumentException("Name parameter cannot be null");
        }
        return (Person)people.get(firstName);
    }

    public Person[] getPeople()
    {
        return originalPeople;
    }

    public void addPerson(Person person) throws Exception
    {
        if (person == null || person.getFirstName() == null || person.getLastName() == null)
        {
            throw new IllegalArgumentException("null person, first name or last name");
        }
        if (person.getFirstName().equals("Ross"))
        {
            throw new Exception("Ross is banned");
        }
        people.put(person.getFirstName(), person);
        logger.debug("Added Person: " + person);
    }

    public Person addPerson(String firstname, String surname) throws Exception
    {
        Person p = new Person(firstname, surname);
        addPerson(p);
        logger.debug("Added Person: " + p);
        return p;
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw RuntimeExceptions.
     */
    public void dispose()
    {
        people.clear();
    }

}
