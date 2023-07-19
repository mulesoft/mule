/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Extension(name = "transactional")
@Xml(prefix = "tx")
@Configurations({TransactionalConfig.class, SdkTransactionalConfig.class})
@Export(classes = org.mule.test.transactional.TransactionalOperations.class)
public class TransactionalExtension {

}
