/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.test.petstore.extension.stereotype.SimpleConnectionStereotype;

@Stereotype(SimpleConnectionStereotype.class)
public class SimplePetStoreConnectionProvider extends PetStoreConnectionProvider<PetStoreClient>
    implements CachedConnectionProvider<PetStoreClient> {

}
