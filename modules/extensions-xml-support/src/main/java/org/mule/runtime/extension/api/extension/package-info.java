/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * W-13829806: Even if this is API, since 4.7.0 this will be no longer be accessible from outside of the Mule Runtime. No user of
 * the Mule Runtime needs to access this classes, these refer to concrete classes looked up with SPI by code in the Mule Runtime.
 */
package org.mule.runtime.extension.api.extension;
