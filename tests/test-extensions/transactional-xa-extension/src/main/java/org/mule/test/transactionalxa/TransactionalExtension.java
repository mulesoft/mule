/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactionalxa;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

@Extension(name = "transactional-xa")
@JavaVersionSupport({JAVA_17, JAVA_21})
@Xml(prefix = "tx-xa")
@Configurations(TransactionalConfig.class)
@Export(classes = org.mule.test.transactionalxa.TransactionalOperations.class)
public class TransactionalExtension {

}
