/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.annotation.api.connector.Connector;

@Extension(name = "petstore", description = "PetStore Test connector")
@Configurations(PetStoreConnectorConfig.class)
@Operations(PetStoreOperations.class)
@Connector(PetStoreClientConnectionHandler.class)
@Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/petstore", namespace = "petstore", schemaVersion = "4.0")
public class PetStoreConnector
{

}
