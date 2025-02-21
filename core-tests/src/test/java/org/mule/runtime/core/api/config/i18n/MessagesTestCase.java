/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.i18n;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authDeniedOnEndpoint;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authEndpointMustSendOrReceive;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authNoCredentials;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authNoEncryptionStrategy;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authNoSecurityProvider;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authSecurityManagerNotSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authSetButNoContext;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authTypeNotRecognised;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotLoadFromClasspath;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotReadPayloadAsBytes;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotReadPayloadAsString;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotSetObjectOnceItHasBeenSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotSetPropertyOnObjectWithParamType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotStartTransaction;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotUseTxAndRemoteSync;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.componentCausedErrorIs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.componentNotRegistered;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.configNotFoundUsage;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.connectorWithProtocolNotRegistered;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.containerAlreadyRegistered;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cryptoFailure;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.descriptorAlreadyExists;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.encryptionStrategyNotSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.endpointIsMalformed;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.eventProcessingFailedFor;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.eventTypeNotRecognised;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.exceptionOnConnectorNoExceptionListener;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.exceptionStackIs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToClone;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToConvertStringUsingEncoding;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreateEndpointFromLocation;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreateManagerInstance;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToDispose;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToGetPooledObject;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToInvoke;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToInvokeRestService;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToLoad;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToLoadTransformer;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToReadFromStore;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToReadPayload;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToScheduleWork;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToStart;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToStop;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToUnregister;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToWriteMessageToStore;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedtoRegisterOnEndpoint;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorInShutdown;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.fatalErrorWhileRunning;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.headerMalformedValueIs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.initialisationFailure;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.interruptedQueuingEventFor;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.managerAlreadyStarted;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.messageIsOfType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.methodWithParamsNotFoundOnObject;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.minMuleVersionNotMet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noCatchAllEndpointSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noComponentForEndpoint;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noCorrelationId;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noCurrentEventForTransformer;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noEndpointsForRouter;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noEntryPointFoundWithArgs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noOutboundRouterSetOn;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.none;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.normalShutdown;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectFailedToInitialise;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectNotFound;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectNotOfCorrectType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertiesNotSet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyIsNotSetOnEvent;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertyIsNotSupportedType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.proxyPoolTimedOut;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.reconnectStrategyFailed;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.resourceManagerDirty;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.resourceManagerNotReady;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.resourceManagerNotStarted;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.responseTimedOutWaitingForId;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.serverShutdownAt;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.serverWasUpForDuration;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.shutdownNormally;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.tooManyAcceptableMethodsOnObjectForTypes;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionAvailableButActionIs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionCanOnlyBindToResources;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionCannotReadState;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionCommitFailed;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionManagerAlreadySet;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionMarkedForRollback;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionNotAvailableButActionIs;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionResourceAlreadyListedForKey;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transactionRollbackFailed;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformFailed;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformFailedBeforeFilter;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformFailedFrom;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformUnexpectedType;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.uniqueIdNotSupportedByAdapter;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.valueIsInvalidFor;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.versionNotSet;

import org.mule.runtime.api.i18n.I18nMessage;
import org.junit.Test;
import org.mule.runtime.api.metadata.DataTypeBuilder;

import java.util.Date;

public class MessagesTestCase {

  @Test
  public void versionNotSetMessage() {
    I18nMessage message = versionNotSet();
    assertThat(message.getMessage(), is("Mule Version Info not set"));
  }

  @Test
  public void serverShutdownAtMessage() {
    I18nMessage message = serverShutdownAt(new Date(2019, 9, 1, 10, 10, 10));
    assertThat(message.getMessage(), is("Server shutdown: 10/1/19, 10:10 AM"));
  }

  @Test
  public void minMuleVersionNotMetMessage() {
    I18nMessage message = minMuleVersionNotMet("4.5.0");
    assertThat(message.getMessage(), is("This module requires at least Mule Version 4.5.0"));
  }

  @Test
  public void shutdownNormallyMessage() {
    I18nMessage message = shutdownNormally(new Date(2020, 9, 3, 10, 10, 10));
    assertThat(message.getMessage(), is("Mule Context shut down normally on: 10/3/20, 10:10 AM"));
  }

  @Test
  public void serverWasUpForDurationMessage() {
    I18nMessage message = serverWasUpForDuration(1000L);
    assertThat(message.getMessage(), is("Server was up for: 0 days, 0 hours, 0 mins, 1.0 sec"));
  }

  @Test
  public void configNotFoundUsageMessage() {
    I18nMessage message = configNotFoundUsage();
    assertThat(message.getMessage(), is(""));
  }

  @Test
  public void fatalErrorWhileRunningMessage() {
    I18nMessage message = fatalErrorWhileRunning();
    assertThat(message.getMessage(), is("A Fatal error has occurred while the server was running:"));
  }

  @Test
  public void exceptionStackIsMessage() {
    I18nMessage message = exceptionStackIs();
    assertThat(message.getMessage(), is("Exception stack is:"));
  }

  @Test
  public void messageIsOfTypeMessage() {
    I18nMessage message = messageIsOfType(MessagesTestCase.class);
    assertThat(message.getMessage(), is("Message payload is of type: MessagesTestCase"));
  }

  @Test
  public void fatalErrorInShutdownMessage() {
    I18nMessage message = fatalErrorInShutdown();
    assertThat(message.getMessage(), is("The error is fatal, the system will shutdown"));
  }

  @Test
  public void normalShutdownMessage() {
    I18nMessage message = normalShutdown();
    assertThat(message.getMessage(), is("The server is shutting down due to normal shutdown request"));
  }

  @Test
  public void noneMessage() {
    I18nMessage message = none();
    assertThat(message.getMessage(), is("None"));
  }

  @Test
  public void notClusteredMessage() {
    I18nMessage message = normalShutdown();
    assertThat(message.getMessage(), is("The server is shutting down due to normal shutdown request"));
  }

  @Test
  public void componentCausedErrorIsMessage() {
    I18nMessage message = componentCausedErrorIs("hello");
    assertThat(message.getMessage(), is("Component that caused exception is: hello"));
  }

  @Test
  public void objectFailedToInitialiseMessage() {
    I18nMessage message = objectFailedToInitialise("world");
    assertThat(message.getMessage(), is("world Failed to initialise"));
  }

  @Test
  public void failedToStopMessage() {
    I18nMessage message = failedToStop("problem");
    assertThat(message.getMessage(), is("Failed to stop problem"));
  }

  @Test
  public void failedToStartMessage() {
    I18nMessage message = failedToStart("problem");
    assertThat(message.getMessage(), is("Failed to start problem"));
  }

  @Test
  public void proxyPoolTimedOutMessage() {
    I18nMessage message = proxyPoolTimedOut();
    assertThat(message.getMessage(), is("Proxy pool timed out"));
  }

  @Test
  public void failedToGetPooledObjectMessage() {
    I18nMessage message = failedToGetPooledObject();
    assertThat(message.getMessage(), is("Failed to borrow object from pool"));
  }

  @Test
  public void componentNotRegisteredMessage() {
    I18nMessage message = componentNotRegistered("The Object");
    assertThat(message.getMessage(), is("Component not registered: The Object"));
  }

  @Test
  public void failedtoRegisterOnEndpointMessage() {
    I18nMessage message = failedtoRegisterOnEndpoint("The Object", "uri/to/name");
    assertThat(message.getMessage(), is("Failed to register listener The Object on endpoint uri/to/name"));
  }

  @Test
  public void failedToUnregisterMessage() {
    I18nMessage message = failedToUnregister("The Object", "uri/to/name");
    assertThat(message.getMessage(), is("Failed to unregister listener The Object on endpoint uri/to/name"));
  }

  @Test
  public void endpointIsMalformedMessage() {
    I18nMessage message = endpointIsMalformed("enpoint");
    assertThat(message.getMessage(), is(""));
  }

  @Test
  public void transformFailedBeforeFilterMessage() {
    I18nMessage message = transformFailedBeforeFilter();
    assertThat(message.getMessage(), is("Failed to transform message before applying the filter"));
  }

  @Test
  public void transformUnexpectedTypeMessage() {
    I18nMessage message = transformUnexpectedType(MessagesTestCase.class, String.class);
    assertThat(message.getMessage(),
               is("The object transformed is of type: \"MessagesTestCase\", but the expected return type is \"String\""));
  }

  @Test
  public void transformFailedFromMessage() {
    I18nMessage message = transformFailedFrom(MessagesTestCase.class);
    assertThat(message.getMessage(),
               is("Failed to transform data from class org.mule.runtime.core.api.config.i18n.MessagesTestCase"));
  }

  @Test
  public void encryptionStrategyNotSetMessage() {
    I18nMessage message = encryptionStrategyNotSet();
    assertThat(message.getMessage(), is(""));
  }

  @Test
  public void failedToLoadTransformerMessage() {
    I18nMessage message = failedToLoadTransformer("The Direction", "The Transformer");
    assertThat(message.getMessage(), is("Failed to load The Direction transformer \"The Transformer\""));
  }

  @Test
  public void failedToLoadMessage() {
    I18nMessage message = failedToLoad("The Object");
    assertThat(message.getMessage(), is("Failed to load The Object"));
  }

  @Test
  public void tooManyAcceptableMethodsOnObjectForTypesMessage() {
    I18nMessage message = tooManyAcceptableMethodsOnObjectForTypes("The Object", String.class);
    assertThat(message.getMessage(),
               is("Found too many possible methods on object \"The Object\" that accept parameters \"java.lang.String\""));
  }

  @Test
  public void cannotSetPropertyOnObjectWithParamTypeMessage() {
    I18nMessage message = cannotSetPropertyOnObjectWithParamType("The Property", MessagesTestCase.class, String.class);
    assertThat(message.getMessage(),
               is("Failed to set property \"The Property\" on object \"org.mule.runtime.core.api.config.i18n.MessagesTestCase\" with parameter type \"java.lang.String\""));
  }

  @Test
  public void noComponentForEndpointMessage() {
    I18nMessage message = noComponentForEndpoint();
    assertThat(message.getMessage(),
               is("Cannot dispatch event, Endpoint is a receiver endpoint and there is no current component"));
  }

  @Test
  public void failedToInvokeMessage() {
    I18nMessage message = failedToInvoke("The Object");
    assertThat(message.getMessage(), is("Failed to invoke The Object"));
  }

  @Test
  public void cannotReadPayloadAsBytesMessage() {
    I18nMessage message = cannotReadPayloadAsBytes("The Object");
    assertThat(message.getMessage(), is("Could not read message payload as bytes (byte[]). Payload type is \"The Object\""));
  }

  @Test
  public void failedToCreateMessage() {
    I18nMessage message = failedToCreate("The Object");
    assertThat(message.getMessage(), is("Failed to create The Object"));
  }

  @Test
  public void noCorrelationIdMessage() {
    I18nMessage message = noCorrelationId();
    assertThat(message.getMessage(), is("There is no Correlation Id set on the message"));
  }

  @Test
  public void failedToDisposeMessage() {
    I18nMessage message = (I18nMessage) failedToDispose("The Object");
    assertThat(message.getMessage(), is("Failed while disposing The Object"));
  }

  @Test
  public void cannotReadPayloadAsStringMessage() {
    I18nMessage message = cannotReadPayloadAsString("The Object");
    assertThat(message.getMessage(), is("Could not read message payload as String. Payload type is \"The Object\""));
  }

  @Test
  public void objectNotFoundMessage() {
    I18nMessage message = objectNotFound("The Object");
    assertThat(message.getMessage(), is("Object \"The Object\" not found in container"));
  }

  @Test
  public void objectIsNullMessage() {
    I18nMessage message = objectIsNull("The Object");
    assertThat(message.getMessage(), is("The required object/property \"The Object\" is null"));
  }

  @Test
  public void transactionMarkedForRollbackMessage() {
    I18nMessage message = transactionMarkedForRollback();
    assertThat(message.getMessage(), is("Cannot commit transaction transaction is marked for rollback"));
  }

  @Test
  public void noCurrentEventForTransformerMessage() {
    I18nMessage message = noCurrentEventForTransformer();
    assertThat(message.getMessage(),
               is("This transformer can only be invoked when an event is being processed. It needs a current event to perform a transform"));
  }

  @Test
  public void initialisationFailureMessage() {
    I18nMessage message = initialisationFailure("The Object");
    assertThat(message.getMessage(), is("Initialisation Failure: The Object"));
  }

  @Test
  public void failedToCreateEndpointFromLocationMessage() {
    I18nMessage message = failedToCreateEndpointFromLocation("The Object");
    assertThat(message.getMessage(), is("Failed to create endpoint from service location: The Object"));
  }

  @Test
  public void managerAlreadyStartedMessage() {
    I18nMessage message = managerAlreadyStarted();
    assertThat(message.getMessage(), is("Manager is already started"));
  }

  @Test
  public void noEndpointsForRouterMessage() {
    I18nMessage message = noEndpointsForRouter();
    assertThat(message.getMessage(), is("No endpoints are set on this router, cannot route message"));
  }

  @Test
  public void responseTimedOutWaitingForIdMessage() {
    I18nMessage message = responseTimedOutWaitingForId(10, 98);
    assertThat(message.getMessage(),
               is("Response timed out (10ms) waiting for message response id \"98\" or this action was interrupted"));
  }

  @Test
  public void failedToWriteMessageToStoreMessage() {
    I18nMessage message = failedToWriteMessageToStore(703, "The Store");
    assertThat(message.getMessage(), is("Failed to write message id \"703\" to store: The Store"));
  }

  @Test
  public void failedToReadFromStoreMessage() {
    I18nMessage message = failedToReadFromStore("The Object");
    assertThat(message.getMessage(), is("Failed to read from store: The Object"));
  }

  @Test
  public void cannotStartTransactionMessage() {
    I18nMessage message = cannotStartTransaction("The Object");
    assertThat(message.getMessage(), is("Can not start The Object transaction"));
  }

  @Test
  public void transactionCommitFailedMessage() {
    I18nMessage message = transactionCommitFailed();
    assertThat(message.getMessage(), is("Transaction commit failed"));
  }

  @Test
  public void transactionRollbackFailedMessage() {
    I18nMessage message = transactionRollbackFailed();
    assertThat(message.getMessage(), is("Transaction rollback failed"));
  }

  @Test
  public void transactionCannotReadStateMessage() {
    I18nMessage message = transactionCannotReadState();
    assertThat(message.getMessage(), is("Unable to read transaction state"));
  }

  @Test
  public void transactionResourceAlreadyListedForKeyMessage() {
    I18nMessage message = transactionResourceAlreadyListedForKey("The Key");
    assertThat(message.getMessage(), is("A resource has already been enlisted for key \"The Key\""));
  }

  @Test
  public void noOutboundRouterSetOnMessage() {
    I18nMessage message = noOutboundRouterSetOn("The Object");
    assertThat(message.getMessage(), is("There is no outbound router configured on component \"The Object\""));
  }

  @Test
  public void transactionAvailableButActionIsMessage() {
    I18nMessage message = transactionAvailableButActionIs("The Action");
    assertThat(message.getMessage(), is("A transaction is available for this session, but transaction action is \"The Action\""));
  }

  @Test
  public void transactionNotAvailableButActionIsMessage() {
    I18nMessage message = transactionNotAvailableButActionIs("The Action");
    assertThat(message.getMessage(),
               is("A transaction is not available for this session, but transaction action is \"The Action\""));
  }

  @Test
  public void noCatchAllEndpointSetMessage() {
    I18nMessage message = noCatchAllEndpointSet();
    assertThat(message.getMessage(), is("Cannot route event in Catch all as no endpoint has been set"));
  }

  @Test
  public void interruptedQueuingEventForMessage() {
    I18nMessage message = interruptedQueuingEventFor("The Object");
    assertThat(message.getMessage(), is("Interrupted while queueing event for \"The Object\""));
  }

  @Test
  public void methodWithParamsNotFoundOnObjectMessage() {
    I18nMessage message = methodWithParamsNotFoundOnObject("startsWith", String.class, Integer.class);
    assertThat(message.getMessage(),
               is("Invocation error. Method \"startsWith\", with parameters \"java.lang.String\" not found on \"java.lang.Integer\""));
  }

  @Test
  public void transformFailedMessage() {
    I18nMessage message = transformFailed("Origin", "Destination");
    assertThat(message.getMessage(), is("Failed to transform from \"Origin\" to \"Destination\""));
  }

  @Test
  public void cryptoFailureMessage() {
    I18nMessage message = cryptoFailure();
    assertThat(message.getMessage(), is("Crypto Failure"));
  }

  @Test
  public void noEntryPointFoundWithArgsMessage() {
    I18nMessage message = noEntryPointFoundWithArgs("The Object", "Arguments");
    assertThat(message.getMessage(), is("Could not find entry point on: \"The Object\" with arguments: \"Arguments\""));
  }

  @Test
  public void authNoSecurityProviderMessage() {
    I18nMessage message = authNoSecurityProvider("The Object");
    assertThat(message.getMessage(), is("There is no Security Provider reqistered called \"The Object\""));
  }

  @Test
  public void transactionCanOnlyBindToResourcesMessage() {
    I18nMessage message = transactionCanOnlyBindToResources("The Object");
    assertThat(message.getMessage(), is("Can only bind \"The Object\" type resources"));
  }

  @Test
  public void cannotLoadFromClasspathMessage() {
    I18nMessage message = cannotLoadFromClasspath("The Object");
    assertThat(message.getMessage(), is("Failed to load \"The Object\" from classpath or file system"));
  }

  @Test
  public void failedToReadPayloadMessage() {
    I18nMessage message = failedToReadPayload();
    assertThat(message.getMessage(), is("Failed to read payload data"));
  }

  @Test
  public void eventProcessingFailedForMessage() {
    I18nMessage message = eventProcessingFailedFor("The Object");
    assertThat(message.getMessage(), is("Failed to process event for component \"The Object\""));
  }

  @Test
  public void authTypeNotRecognisedMessage() {
    I18nMessage message = authTypeNotRecognised("The Object");
    assertThat(message.getMessage(), is("The authentication type The Object is not recognised by the Security Manager"));
  }

  @Test
  public void authSecurityManagerNotSetMessage() {
    I18nMessage message = authSecurityManagerNotSet();
    assertThat(message.getMessage(), is("A Security Manager has not been configured on this Mule instance"));
  }

  @Test
  public void authSetButNoContextMessage() {
    I18nMessage message = authSetButNoContext("The Object");
    assertThat(message.getMessage(),
               is("Registered authentication is set to The Object but there was no security context on the session"));
  }

  @Test
  public void authDeniedOnEndpointMessage() {
    I18nMessage message = authDeniedOnEndpoint("The Object");
    assertThat(message.getMessage(), is("Authentication denied on connector The Object"));
  }

  @Test
  public void authFailedForUserMessage() {
    I18nMessage message = authFailedForUser("The User");
    assertThat(message.getMessage(), is("Authentication failed for principal The User"));
  }

  @Test
  public void authEndpointMustSendOrReceiveMessage() {
    I18nMessage message = authEndpointMustSendOrReceive();
    assertThat(message.getMessage(),
               is("The Endpoint that this security filter is associated with must be able to send or receive (it cannot be global)."));
  }

  @Test
  public void transactionManagerAlreadySetMessage() {
    I18nMessage message = transactionManagerAlreadySet();
    assertThat(message.getMessage(), is("The transaction manager on the MuleManager cannot be set one has already been set"));
  }

  @Test
  public void failedToCreateManagerInstanceMessage() {
    I18nMessage message = failedToCreateManagerInstance("The Class");
    assertThat(message.getMessage(), is("Failed to create Manager instance \"The Class\""));
  }

  @Test
  public void failedToCloneMessage() {
    I18nMessage message = failedToClone("The Object");
    assertThat(message.getMessage(), is("Failed to clone The Object"));
  }

  @Test
  public void exceptionOnConnectorNoExceptionListenerMessage() {
    I18nMessage message = exceptionOnConnectorNoExceptionListener("The Object");
    assertThat(message.getMessage(),
               is("Exception occurred on connector \"The Object\". Exception listener is not set. This could result in message loss"));
  }

  @Test
  public void uniqueIdNotSupportedByAdapterMessage() {
    I18nMessage message = uniqueIdNotSupportedByAdapter("The Object");
    assertThat(message.getMessage(), is("Adapter \"The Object\" does not support unique identifiers"));
  }

  @Test
  public void failedToScheduleWorkMessage() {
    I18nMessage message = failedToScheduleWork();
    assertThat(message.getMessage(), is("Failed to schedule work with the Work manager"));
  }

  @Test
  public void authNoCredentialsMessage() {
    I18nMessage message = authNoCredentials();
    assertThat(message.getMessage(), is("No credentials set"));
  }

  @Test
  public void valueIsInvalidForMessage() {
    I18nMessage message = valueIsInvalidFor("The Object", "The Parameter");
    assertThat(message.getMessage(),
               is("The value \"The Object\" is invalid for property \"The Parameter\", check the relevant documentation"));
  }

  @Test
  public void connectorWithProtocolNotRegisteredMessage() {
    I18nMessage message = connectorWithProtocolNotRegistered("The Object");
    assertThat(message.getMessage(), is("A connector with protocol \"The Object\" is not registered with this Mule instance"));
  }

  @Test
  public void propertyIsNotSupportedTypeMessage() {
    I18nMessage message = propertyIsNotSupportedType("The Object", MessagesTestCase.class, String.class);
    assertThat(message.getMessage(),
               is("Object \"The Object\" is not of supported type \"org.mule.runtime.core.api.config.i18n.MessagesTestCase\" it is of type \"java.lang.String\""));
  }

  @Test
  public void containerAlreadyRegisteredMessage() {
    I18nMessage message = containerAlreadyRegistered("The Object");
    assertThat(message.getMessage(), is("A container is already registered with the name \"The Object\""));
  }

  @Test
  public void resourceManagerNotStartedMessage() {
    I18nMessage message = resourceManagerNotStarted();
    assertThat(message.getMessage(), is("Resource manager has not been started"));
  }

  @Test
  public void resourceManagerDirtyMessage() {
    I18nMessage message = resourceManagerDirty();
    assertThat(message.getMessage(),
               is("Resource manager is set to dirty, this *may* mean it is corrupt. No modifications are allowed until a recovery run has been performed!"));
  }

  @Test
  public void resourceManagerNotReadyMessage() {
    I18nMessage message = resourceManagerNotReady();
    assertThat(message.getMessage(), is("Resource manager is not ready"));
  }

  @Test
  public void reconnectStrategyFailedMessage() {
    I18nMessage message = reconnectStrategyFailed(MessagesTestCase.class, "The description");
    assertThat(message.getMessage(),
               is("ReconnectStrategy \"org.mule.runtime.core.api.config.i18n.MessagesTestCase\" failed to reconnect receiver on endpoint \"The description\""));
  }

  @Test
  public void cannotSetObjectOnceItHasBeenSetMessage() {
    I18nMessage message = cannotSetObjectOnceItHasBeenSet("The Object");
    assertThat(message.getMessage(), is("Cannot not set property \"The Object\" once it has already been set"));
  }

  @Test
  public void eventTypeNotRecognisedMessage() {
    I18nMessage message = eventTypeNotRecognised("The Object");
    assertThat(message.getMessage(), is("Event type \"The Object\" not recognised by this event manager"));
  }

  @Test
  public void propertyIsNotSetOnEventMessage() {
    I18nMessage message = propertyIsNotSetOnEvent("The Object");
    assertThat(message.getMessage(), is("The required property \"The Object\" is not set on the event"));
  }

  @Test
  public void descriptorAlreadyExistsMessage() {
    I18nMessage message = descriptorAlreadyExists("The Object");
    assertThat(message.getMessage(), is("Descriptor \"The Object\" already exists in the model"));
  }

  @Test
  public void failedToInvokeRestServiceMessage() {
    I18nMessage message = failedToInvokeRestService("The Object");
    assertThat(message.getMessage(), is("Failed to invoke REST service \"The Object\""));
  }

  @Test
  public void authNoEncryptionStrategyMessage() {
    I18nMessage message = authNoEncryptionStrategy("The Object");
    assertThat(message.getMessage(), is("There is no Encryption Strategy registered called \"The Object\""));
  }

  @Test
  public void headerMalformedValueIsMessage() {
    I18nMessage message = headerMalformedValueIs("The Header", "The value");
    assertThat(message.getMessage(), is("Header field \"The Header\" is malformed. Value is \"The value\""));
  }

  @Test
  public void cannotUseTxAndRemoteSyncMessage() {
    I18nMessage message = cannotUseTxAndRemoteSync();
    assertThat(message.getMessage(),
               is("An endpoint cannot use remote Sync when it is transacted. Check your endpoint configuration"));
  }

  @Test
  public void propertiesNotSetMessage() {
    I18nMessage message = propertiesNotSet("The Object");
    assertThat(message.getMessage(), is("Property \"The Object\" not set.  One or more of them must be set"));
  }

  @Test
  public void objectNotOfCorrectTypeMessage() {
    I18nMessage message = objectNotOfCorrectType(MessagesTestCase.class, String.class);
    assertThat(message.getMessage(),
               is("Object \"org.mule.runtime.core.api.config.i18n.MessagesTestCase\" not of correct type. It must be of type \"java.lang.String\""));
  }

  @Test
  public void failedToConvertStringUsingEncodingMessage() {
    I18nMessage message = failedToConvertStringUsingEncoding("The Object");
    assertThat(message.getMessage(), is("Failed to convert a string using the The Object encoding"));
  }



}
