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
package org.mule.test.integration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.components.simple.EchoService;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.lifecycle.Disposable;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>TestServiceComponent</code> is a test WebServices component
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestServiceComponent extends FunctionalTestComponent implements EchoService,
        DateService, PeopleService, Disposable
{
    private static transient Log logger = LogFactory.getLog(FunctionalTestComponent.class);

    private static Map people = new HashMap();

    public TestServiceComponent()
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
        if(firstName==null ||"".equals(firstName)) {
            throw new NullPointerException("Name parameter cannot be null");
        }
        Person p = (Person) people.get(firstName);
        return p;
    }

    public Person[] getPeople()
    {
        Person[] p = new Person[people.size()];
        int i = 0;
        for (Iterator iterator = people.values().iterator(); iterator.hasNext(); i++) {
            p[i] = (Person) iterator.next();

        }
        return p;
    }

    public void addPerson(Person person) throws Exception
    {
        if (person == null || person.getFirstName() == null || person.getLastName() == null) {
            throw new IllegalArgumentException("null person, first name or last name");
        }
        if (person.getFirstName().equals("Ross")) {
            throw new Exception("Ross is banned");
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
     * A lifecycle method where implementor should free up any resources If an
     * exception is thrown it should just be logged and processing should
     * continue. This method should not throw Runtime exceptions
     */
    public void dispose()
    {
        people.clear();
    }
}
