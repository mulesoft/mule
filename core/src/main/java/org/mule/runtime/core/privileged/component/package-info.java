/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * These package needs to be accessible from the classloader of the mule-extensions, because the dynamic classes generated with
 * bytebuddy reference this package classes, and those dynamic classes exist within the classloader of the mule-extension.
 */
package org.mule.runtime.core.privileged.component;
