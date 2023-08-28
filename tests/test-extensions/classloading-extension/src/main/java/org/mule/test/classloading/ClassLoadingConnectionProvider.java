/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import org.mule.runtime.api.connection.ConnectionProvider;

// This is an internal interface created just so this extension connection providers do not inherit
// directly from ConnectionProvider so when the proxy for the provider to enforce the usage of the extension class loader
// in each method call gets created we validate that it won't use this internal interface but the one in the runtime API only.
// See MULE-13922 which was this case and fail because an internal class from the extension was discovered as interface and failed
// when creating the dynamic class.
public interface ClassLoadingConnectionProvider<C> extends ConnectionProvider<C> {

}
