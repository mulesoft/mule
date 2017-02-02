/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

/**
 * Defines the behaviours that are supported when looking for a destination based
 * on its name while using a JNDI name resolver.
 *
 * NEVER: Will never lookup for jndi destinations.
 * ALWAYS: Will always lookup the destinations through JNDI. It will fail if the destination does not exists.
 * TRY_ALWAYS: Will always try to lookup the destinations through JNDI but if it does not exists it will create a new one.
 *
 * @since 4.0
 */
public enum LookupJndiDestination {
  NEVER, ALWAYS, TRY_ALWAYS
}
