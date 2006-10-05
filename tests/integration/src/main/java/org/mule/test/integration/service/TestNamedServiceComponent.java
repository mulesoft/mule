/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.lifecycle.Disposable;

/**
 * <code>TestServiceComponent</code> is a test WebServices component
 */
public class TestNamedServiceComponent extends FunctionalTestComponent
    implements org.mule.components.simple.EchoService, DateService, PeopleService, Disposable
{
    private final Map people = Collections.synchronizedMap(new HashMap());

    public TestNamedServiceComponent()
    {
        people.put("Barney", new Person("Barney", "Rubble"));
        people.put("Fred", new Person("Fred", "Flintstone"));
        people.put("Wilma", new Person("Wilma", "Flintstone"));
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
        return (Person)people.get(firstName);
    }

    public Person[] getPeople()
    {
        return (Person[])IteratorUtils.toArray(people.values().iterator(), Person.class);
    }

    public void addPerson(Person person) throws Exception
    {
        if (person == null || person.getFirstName() == null || person.getLastName() == null)
        {
            throw new IllegalArgumentException("null person, first name or last name");
        }
        if (person.getFirstName().equals("Nodet"))
        {
            throw new Exception("Nodet is banned");
        }
        people.put(person.getFirstName(), person);
        logger.info("Added Person: " + person);
    }

    public Person addPerson(String firstname, String surname) throws Exception
    {
        Person p = new Person(firstname, surname);
        addPerson(p);
        logger.info("Added Person: " + p);
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
