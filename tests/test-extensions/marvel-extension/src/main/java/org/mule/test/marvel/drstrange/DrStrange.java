/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

/**
 * Default extension to test cursor streams and lists. Given Dr. Strange's ability to use the eye of Agamotto
 * to reverse and advance time, he's uniquely positioned to manipulate such streams
 */
@Configuration(name = "dr-strange")
@Operations(DrStrangeOperations.class)
@Sources(DrStrangeBytesSource.class)
@ConnectionProviders(MysticConnectionProvider.class)
public class DrStrange {

}
