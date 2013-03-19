/**
 * Mule Development Kit
 * Copyright 2010-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */


package org.mule.api;

/**
 * List of possible outcomes to a connection failure
 */
public enum ConnectionExceptionCode {
    /**
     * The host cannot be resolved to an IP address
     */
    UNKNOWN_HOST,
    /**
     * The destination cannot be reached. Either the host is wrong
     * or the port might be.
     */
    CANNOT_REACH,
    /**
     * The supplied credentials are not correct.
     */
    INCORRECT_CREDENTIALS,
    /**
     * The credentials used to authenticate has expired.
     */
    CREDENTIALS_EXPIRED,
    /**
     * Something else went wrong.
     */
    UNKNOWN;
}
