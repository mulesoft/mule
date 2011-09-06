/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

/**
 * Defines a socket port number that will be dynamically assigned as an
 * external resource. The instance will check that the port has been released
 * on test shutdown.
 * To use an instance dynamic socket port:
 * <pre>
 *     @Rule
 *     public DynamicSocketPortNumber serverPort = new DynamicSocketPortNumber("server_port");
 * </pre>
 * <p/>
 * In order to use static dynamic ports:
 * <p/>
 * <pre>
 *     @ClassRule
 *     public static DynamicPort dynamicPort;
 * </pre>
 */
public class DynamicPort extends ExternalResource
{

    final static private int MIN_PORT = 5000;
    final static private int MAX_PORT = 6000;

    protected static FreePortFinder freePortFinder = new FreePortFinder(MIN_PORT, MAX_PORT);

    protected Log logger = LogFactory.getLog(getClass());

    private final String name;
    private int number;
    private boolean initialized = false;

    /**
     * Creates a dynamic port resource for a given port name.
     *
     * @param name the name assigned to the port number. On resource creation
     *             a new system property will be created with that name and the
     *             value will be the port number.
     */
    public DynamicPort(String name)
    {
        this.name = name;
    }

    /**
     * Initializes the dynamic port.
     * <p/>
     * NOTE: this method was made public in order to support the usage of
     * static dynamic ports because current JUnit version does not support
     * class rules.
     *
     * @throws Throwable
     */
    @Override
    public void before() throws Throwable
    {
        if (initialized)
        {
            throw new IllegalArgumentException("Dynamic port was already initialized");
        }

        number = freePortFinder.find();
        System.setProperty(name, String.valueOf(number));
        initialized = true;
    }

    /**
     * Checks that the port has been released. For now if it was not released it
     * just logs a message so we can track the problem.
     * <p/>
     * NOTE: this method was made public in order to support the usage of
     * static dynamic ports because current JUnit version does not support
     * class rules.
     *
     * @throws Throwable
     */
    @Override
    public void after()
    {
        if (!initialized)
        {
            throw new IllegalArgumentException("Dynamic port was not initialized");
        }

        freePortFinder.releasePort(number);
        initialized = false;
    }

    public int getNumber()
    {
        return number;
    }

    public String getName()
    {
        return name;
    }
}
