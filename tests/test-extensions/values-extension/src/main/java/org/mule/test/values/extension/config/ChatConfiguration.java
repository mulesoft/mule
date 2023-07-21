/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.test.values.extension.ChatOperations;
import org.mule.test.values.extension.connection.ChatConnectionProvider;

@Configuration(name = "chat")
@ConnectionProviders(ChatConnectionProvider.class)
@Operations(ChatOperations.class)
public class ChatConfiguration {

}
