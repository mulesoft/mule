/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.test.petstore.extension.stereotype.SimpleConnectionStereotype;

@Stereotype(SimpleConnectionStereotype.class)
public class SimplePetStoreConnectionProvider extends PetStoreConnectionProvider<PetStoreClient>
    implements CachedConnectionProvider<PetStoreClient> {

}
