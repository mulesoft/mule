/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.i18n;

import static org.apache.commons.lang3.StringUtils.defaultString;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.internal.util.DateUtils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

public class CoreMessages extends I18nMessageFactory {

  private static final CoreMessages factory = new CoreMessages();

  private static final String BUNDLE_PATH = getBundlePath("core");

  public static I18nMessage versionNotSet() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }

  public static I18nMessage serverStartedAt(long startDate) {
    return factory.createMessage(BUNDLE_PATH, 2, new Date(startDate));
  }

  public static I18nMessage serverShutdownAt(Date date) {
    return factory.createMessage(BUNDLE_PATH, 3, date);
  }

  public static I18nMessage notSet() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage version() {
    String version = defaultString(MuleManifest.getProductVersion(), notSet().getMessage());
    return factory.createMessage(BUNDLE_PATH, 6, version);
  }

  public static I18nMessage minMuleVersionNotMet(String minVersion) {
    return factory.createMessage(BUNDLE_PATH, 344, minVersion);
  }

  public static I18nMessage shutdownNormally(Date date) {
    return factory.createMessage(BUNDLE_PATH, 7, date);
  }

  public static I18nMessage serverWasUpForDuration(long duration) {
    String formattedDuration = DateUtils.getFormattedDuration(duration);
    return factory.createMessage(BUNDLE_PATH, 8, formattedDuration);
  }

  public static I18nMessage configNotFoundUsage() {
    return factory.createMessage(BUNDLE_PATH, 9);
  }

  public static I18nMessage fatalErrorWhileRunning() {
    return factory.createMessage(BUNDLE_PATH, 10);
  }

  public static I18nMessage rootStackTrace() {
    return factory.createMessage(BUNDLE_PATH, 11);
  }

  public static I18nMessage exceptionStackIs() {
    return factory.createMessage(BUNDLE_PATH, 12);
  }

  public static I18nMessage messageIsOfType(Class<?> type) {
    return factory.createMessage(BUNDLE_PATH, 18, ClassUtils.getSimpleName(type));
  }

  public static I18nMessage fatalErrorInShutdown() {
    return factory.createMessage(BUNDLE_PATH, 20);
  }

  public static I18nMessage normalShutdown() {
    return factory.createMessage(BUNDLE_PATH, 21);
  }

  public static I18nMessage none() {
    return factory.createMessage(BUNDLE_PATH, 22);
  }

  public static I18nMessage notClustered() {
    return factory.createMessage(BUNDLE_PATH, 23);
  }

  public static I18nMessage failedToRouterViaEndpoint(Processor target) {
    return factory.createMessage(BUNDLE_PATH, 30, target);
  }

  public static I18nMessage componentCausedErrorIs(Object component) {
    return factory.createMessage(BUNDLE_PATH, 38, component);
  }

  public static I18nMessage objectFailedToInitialise(String string) {
    return factory.createMessage(BUNDLE_PATH, 40, string);
  }

  public static I18nMessage failedToStop(String string) {
    return factory.createMessage(BUNDLE_PATH, 41, string);
  }

  public static I18nMessage failedToStart(String string) {
    return factory.createMessage(BUNDLE_PATH, 42, string);
  }

  public static I18nMessage proxyPoolTimedOut() {
    return factory.createMessage(BUNDLE_PATH, 43);
  }

  public static I18nMessage failedToGetPooledObject() {
    return factory.createMessage(BUNDLE_PATH, 44);
  }

  public static I18nMessage objectIsNull(String string) {
    return factory.createMessage(BUNDLE_PATH, 45, string);
  }

  public static I18nMessage componentNotRegistered(String name) {
    return factory.createMessage(BUNDLE_PATH, 46, name);
  }

  public static I18nMessage failedtoRegisterOnEndpoint(String name, Object endpointURI) {
    return factory.createMessage(BUNDLE_PATH, 47, name, endpointURI);
  }

  public static I18nMessage failedToUnregister(String name, Object endpointURI) {
    return factory.createMessage(BUNDLE_PATH, 48, name, endpointURI);
  }

  public static I18nMessage endpointIsMalformed(String endpoint) {
    return factory.createMessage(BUNDLE_PATH, 51, endpoint);
  }

  public static I18nMessage transformFailedBeforeFilter() {
    return factory.createMessage(BUNDLE_PATH, 52);
  }

  public static I18nMessage transformUnexpectedType(Class<?> class1, Class<?> returnClass) {
    return factory.createMessage(BUNDLE_PATH, 53, ClassUtils.getSimpleName(class1), ClassUtils.getSimpleName(returnClass));
  }

  public static I18nMessage transformUnexpectedType(DataType dt1, DataType dt2) {
    return factory.createMessage(BUNDLE_PATH, 53, dt1, dt2);
  }

  public static I18nMessage transformOnObjectUnsupportedTypeOfEndpoint(String name, Class<?> class1) {
    return factory.createMessage(BUNDLE_PATH, 54, name, StringMessageUtils.toString(class1));
  }

  public static I18nMessage transformFailedFrom(Class<?> clazz) {
    return factory.createMessage(BUNDLE_PATH, 55, clazz);
  }

  public static I18nMessage encryptionStrategyNotSet() {
    return factory.createMessage(BUNDLE_PATH, 56);
  }

  public static I18nMessage failedToLoadTransformer(String direction, String transformer) {
    return factory.createMessage(BUNDLE_PATH, 57, direction, transformer);
  }

  public static I18nMessage failedToLoad(String string) {
    return factory.createMessage(BUNDLE_PATH, 58, string);
  }

  public static I18nMessage tooManyAcceptableMethodsOnObjectForTypes(Object object, Object types) {
    return factory.createMessage(BUNDLE_PATH, 60, StringMessageUtils.toString(object), StringMessageUtils.toString(types));
  }

  public static I18nMessage cannotSetPropertyOnObjectWithParamType(String property, Class<?> class1, Class<?> class2) {
    return factory.createMessage(BUNDLE_PATH, 61, property, StringMessageUtils.toString(class1),
                                 StringMessageUtils.toString(class2));
  }

  public static I18nMessage noComponentForEndpoint() {
    return factory.createMessage(BUNDLE_PATH, 64);
  }

  public static I18nMessage failedToCreate(String string) {
    return factory.createMessage(BUNDLE_PATH, 65, string);
  }

  public static I18nMessage noCorrelationId() {
    return factory.createMessage(BUNDLE_PATH, 66);
  }

  public static Object failedToDispose(String string) {
    return factory.createMessage(BUNDLE_PATH, 67, string);
  }

  public static I18nMessage failedToInvoke(String string) {
    return factory.createMessage(BUNDLE_PATH, 68, string);
  }

  public static I18nMessage cannotReadPayloadAsBytes(String type) {
    return factory.createMessage(BUNDLE_PATH, 69, type);
  }

  public static I18nMessage cannotReadPayloadAsString(String type) {
    return factory.createMessage(BUNDLE_PATH, 70, type);
  }

  public static I18nMessage objectNotFound(Object object) {
    return factory.createMessage(BUNDLE_PATH, 76, object);
  }

  public static I18nMessage transactionMarkedForRollback() {
    return factory.createMessage(BUNDLE_PATH, 77);
  }

  public static I18nMessage transactionCannotBindToNullKey() {
    return factory.createMessage(BUNDLE_PATH, 78);
  }

  public static I18nMessage transactionCannotBindNullResource() {
    return factory.createMessage(BUNDLE_PATH, 79);
  }

  public static I18nMessage transactionSingleResourceOnly() {
    return factory.createMessage(BUNDLE_PATH, 80);
  }

  public static I18nMessage noCurrentEventForTransformer() {
    return factory.createMessage(BUNDLE_PATH, 81);
  }

  public static I18nMessage objectNotRegistered(String type, String name) {
    return factory.createMessage(BUNDLE_PATH, 82, type, name);
  }

  public static I18nMessage initialisationFailure(String string) {
    return factory.createMessage(BUNDLE_PATH, 85, string);
  }

  public static I18nMessage failedToCreateEndpointFromLocation(String string) {
    return factory.createMessage(BUNDLE_PATH, 87, string);
  }

  public static I18nMessage managerAlreadyStarted() {
    return factory.createMessage(BUNDLE_PATH, 88);
  }

  public static I18nMessage noEndpointsForRouter() {
    return factory.createMessage(BUNDLE_PATH, 89);
  }

  public static I18nMessage responseTimedOutWaitingForId(int timeout, Object id) {
    return factory.createMessage(BUNDLE_PATH, 90, String.valueOf(timeout), id);
  }

  public static I18nMessage failedToWriteMessageToStore(Object id, String storeName) {
    return factory.createMessage(BUNDLE_PATH, 94, id, storeName);
  }

  public static I18nMessage failedToReadFromStore(String absolutePath) {
    return factory.createMessage(BUNDLE_PATH, 95, absolutePath);
  }

  public static I18nMessage cannotStartTransaction(String string) {
    return factory.createMessage(BUNDLE_PATH, 96, string);
  }

  public static I18nMessage transactionCommitFailed() {
    return factory.createMessage(BUNDLE_PATH, 97);
  }

  public static I18nMessage transactionRollbackFailed() {
    return factory.createMessage(BUNDLE_PATH, 98);
  }

  public static I18nMessage transactionCannotReadState() {
    return factory.createMessage(BUNDLE_PATH, 99);
  }

  public static I18nMessage transactionResourceAlreadyListedForKey(Object key) {
    return factory.createMessage(BUNDLE_PATH, 100, key);
  }

  public static I18nMessage noOutboundRouterSetOn(String string) {
    return factory.createMessage(BUNDLE_PATH, 101, string);
  }

  public static I18nMessage transactionAvailableButActionIs(String string) {
    return factory.createMessage(BUNDLE_PATH, 103, string);
  }

  public static I18nMessage transactionNotAvailableButActionIs(String string) {
    return factory.createMessage(BUNDLE_PATH, 104, string);
  }

  public static I18nMessage noCatchAllEndpointSet() {
    return factory.createMessage(BUNDLE_PATH, 105);
  }

  public static I18nMessage interruptedQueuingEventFor(Object object) {
    return factory.createMessage(BUNDLE_PATH, 106, object);
  }

  public static I18nMessage transactionCannotUnbind() {
    return factory.createMessage(BUNDLE_PATH, 107);
  }

  public static I18nMessage transactionAlreadyBound() {
    return factory.createMessage(BUNDLE_PATH, 108);
  }

  public static I18nMessage methodWithParamsNotFoundOnObject(String method, Object class1, Class<?> class2) {
    return factory.createMessage(BUNDLE_PATH, 109, method, StringMessageUtils.toString(class1),
                                 StringMessageUtils.toString(class2));
  }

  public static I18nMessage transformFailed(String from, String to) {
    return factory.createMessage(BUNDLE_PATH, 110, from, to);
  }

  public static I18nMessage transformFailed(String from, DataType to) {
    return transformFailed(from, to.getClass().getName());
  }

  public static I18nMessage cryptoFailure() {
    return factory.createMessage(BUNDLE_PATH, 112);
  }

  public static I18nMessage noEntryPointFoundWithArgs(Object object, Object args) {
    return factory.createMessage(BUNDLE_PATH, 116, StringMessageUtils.toString(object), StringMessageUtils.toString(args));
  }

  public static I18nMessage authNoSecurityProvider(String providerName) {
    return factory.createMessage(BUNDLE_PATH, 117, providerName);
  }

  public static I18nMessage transactionCanOnlyBindToResources(String string) {
    return factory.createMessage(BUNDLE_PATH, 120, string);
  }

  public static I18nMessage cannotLoadFromClasspath(String string) {
    return factory.createMessage(BUNDLE_PATH, 122, string);
  }

  public static I18nMessage failedToReadPayload() {
    return factory.createMessage(BUNDLE_PATH, 124);
  }

  public static I18nMessage eventProcessingFailedFor(String name) {
    return factory.createMessage(BUNDLE_PATH, 127, name);
  }

  public static I18nMessage authTypeNotRecognised(String string) {
    return factory.createMessage(BUNDLE_PATH, 131, string);
  }

  public static I18nMessage authSecurityManagerNotSet() {
    return factory.createMessage(BUNDLE_PATH, 132);
  }

  public static I18nMessage authSetButNoContext(String name) {
    return factory.createMessage(BUNDLE_PATH, 133, name);
  }

  public static I18nMessage authDeniedOnEndpoint(String connectorName) {
    return factory.createMessage(BUNDLE_PATH, 134, connectorName);
  }

  public static I18nMessage authFailedForUser(Object user) {
    return factory.createMessage(BUNDLE_PATH, 135, user);
  }

  public static I18nMessage authEndpointMustSendOrReceive() {
    return factory.createMessage(BUNDLE_PATH, 136);
  }

  public static I18nMessage transactionManagerAlreadySet() {
    return factory.createMessage(BUNDLE_PATH, 140);
  }

  public static I18nMessage failedToCreateManagerInstance(String className) {
    return factory.createMessage(BUNDLE_PATH, 144, className);
  }

  public static I18nMessage failedToClone(String string) {
    return factory.createMessage(BUNDLE_PATH, 145, string);
  }

  public static I18nMessage exceptionOnConnectorNoExceptionListener(String name) {
    return factory.createMessage(BUNDLE_PATH, 146, name);
  }

  public static I18nMessage uniqueIdNotSupportedByAdapter(String name) {
    return factory.createMessage(BUNDLE_PATH, 147, name);
  }

  public static I18nMessage serverNotificationManagerNotEnabled() {
    return factory.createMessage(BUNDLE_PATH, 150);
  }

  public static I18nMessage failedToScheduleWork() {
    return factory.createMessage(BUNDLE_PATH, 151);
  }

  public static I18nMessage authNoCredentials() {
    return factory.createMessage(BUNDLE_PATH, 152);
  }

  public static I18nMessage valueIsInvalidFor(String value, String parameter) {
    return factory.createMessage(BUNDLE_PATH, 154, value, parameter);
  }

  public static I18nMessage connectorWithProtocolNotRegistered(String scheme) {
    return factory.createMessage(BUNDLE_PATH, 156, scheme);
  }

  public static I18nMessage propertyIsNotSupportedType(String property, Class<?> expected, Class<?> actual) {
    return factory.createMessage(BUNDLE_PATH, 157, property, StringMessageUtils.toString(expected),
                                 StringMessageUtils.toString(actual));
  }

  public static I18nMessage propertyIsNotSupportedType(String property, Class<?>[] expected, Class<?> actual) {
    return factory.createMessage(BUNDLE_PATH, 157, property, StringMessageUtils.toString(expected),
                                 StringMessageUtils.toString(actual));
  }

  public static I18nMessage containerAlreadyRegistered(String name) {
    return factory.createMessage(BUNDLE_PATH, 155, name);
  }

  public static I18nMessage resourceManagerNotStarted() {
    return factory.createMessage(BUNDLE_PATH, 161);
  }

  public static I18nMessage resourceManagerDirty() {
    return factory.createMessage(BUNDLE_PATH, 162);
  }

  public static I18nMessage resourceManagerNotReady() {
    return factory.createMessage(BUNDLE_PATH, 163);
  }

  public static I18nMessage reconnectStrategyFailed(Class<?> strategy, String description) {
    return factory.createMessage(BUNDLE_PATH, 164, StringMessageUtils.toString(strategy), description);
  }

  public static I18nMessage cannotSetObjectOnceItHasBeenSet(String string) {
    return factory.createMessage(BUNDLE_PATH, 165, string);
  }

  public static I18nMessage eventTypeNotRecognised(String string) {
    return factory.createMessage(BUNDLE_PATH, 166, string);
  }

  public static I18nMessage isStopped(String name) {
    return factory.createMessage(BUNDLE_PATH, 167, name);
  }

  public static I18nMessage propertyIsNotSetOnEvent(String property) {
    return factory.createMessage(BUNDLE_PATH, 168, property);
  }

  public static I18nMessage descriptorAlreadyExists(String name) {
    return factory.createMessage(BUNDLE_PATH, 171, name);
  }

  public static I18nMessage failedToInvokeRestService(String service) {
    return factory.createMessage(BUNDLE_PATH, 172, service);
  }

  public static I18nMessage authNoEncryptionStrategy(String strategyName) {
    return factory.createMessage(BUNDLE_PATH, 174, strategyName);
  }

  public static I18nMessage headerMalformedValueIs(String header, String value) {
    return factory.createMessage(BUNDLE_PATH, 175, header, value);
  }

  public static I18nMessage transformOnObjectNotOfSpecifiedType(DataType resultType, Object expectedType) {
    return factory.createMessage(BUNDLE_PATH, 177, resultType.getType().getName(), expectedType.getClass());
  }

  public static I18nMessage cannotUseTxAndRemoteSync() {
    return factory.createMessage(BUNDLE_PATH, 178);
  }

  public static I18nMessage failedToBuildMessage() {
    return factory.createMessage(BUNDLE_PATH, 180);
  }

  public static I18nMessage propertiesNotSet(String string) {
    return factory.createMessage(BUNDLE_PATH, 183, string);
  }

  public static I18nMessage objectNotOfCorrectType(Class<?> actualClass, Class<?>[] expectedClass) {
    return factory.createMessage(BUNDLE_PATH, 185, StringMessageUtils.toString(actualClass),
                                 StringMessageUtils.toString(expectedClass));
  }

  public static I18nMessage objectNotOfCorrectType(Class<?> actualClass, Class<?> expectedClass) {
    return factory.createMessage(BUNDLE_PATH, 185, StringMessageUtils.toString(actualClass),
                                 StringMessageUtils.toString(expectedClass));
  }

  public static I18nMessage failedToConvertStringUsingEncoding(String encoding) {
    return factory.createMessage(BUNDLE_PATH, 188, encoding);
  }

  public static I18nMessage propertyHasInvalidValue(String property, Object value) {
    return factory.createMessage(BUNDLE_PATH, 189, property, value);
  }

  public static I18nMessage schemeCannotChangeForRouter(String scheme, String scheme2) {
    return factory.createMessage(BUNDLE_PATH, 192, scheme, scheme2);
  }

  public static I18nMessage days() {
    return factory.createMessage(BUNDLE_PATH, 193);
  }

  public static I18nMessage hours() {
    return factory.createMessage(BUNDLE_PATH, 194);
  }

  public static I18nMessage minutes() {
    return factory.createMessage(BUNDLE_PATH, 195);
  }

  public static I18nMessage seconds() {
    return factory.createMessage(BUNDLE_PATH, 196);
  }

  public static I18nMessage templateCausedMalformedEndpoint(String uri, String newUri) {
    return factory.createMessage(BUNDLE_PATH, 197, uri, newUri);
  }

  public static I18nMessage couldNotDetermineDestinationComponentFromEndpoint(String endpoint) {
    return factory.createMessage(BUNDLE_PATH, 198, endpoint);
  }

  public static I18nMessage sessionValueIsMalformed(String string) {
    return factory.createMessage(BUNDLE_PATH, 201, string);
  }

  public static I18nMessage streamingFailedNoStream() {
    return factory.createMessage(BUNDLE_PATH, 205);
  }

  public static I18nMessage failedToReadAttachment(String string) {
    return factory.createMessage(BUNDLE_PATH, 207, string);
  }

  public static I18nMessage failedToInitSecurityProvider(String providerClass) {
    return factory.createMessage(BUNDLE_PATH, 208, providerClass);
  }

  public static I18nMessage streamingNotSupported(String protocol) {
    return factory.createMessage(BUNDLE_PATH, 209, protocol);
  }

  public static I18nMessage streamingComponentMustHaveOneEndpoint(String name) {
    return factory.createMessage(BUNDLE_PATH, 210, name);
  }

  public static I18nMessage streamingFailedForEndpoint(String string) {
    return factory.createMessage(BUNDLE_PATH, 212, string);
  }

  public static I18nMessage streamingEndpointsDoNotSupportTransformers() {
    return factory.createMessage(BUNDLE_PATH, 213);
  }

  public static I18nMessage streamingEndpointsMustBeUsedWithStreamingModel() {
    return factory.createMessage(BUNDLE_PATH, 214);
  }

  public static I18nMessage tooManyMatchingMethodsOnObjectWhichReturn(Object object, Object returnType) {
    return factory.createMessage(BUNDLE_PATH, 216, StringMessageUtils.toString(object), StringMessageUtils.toString(returnType));
  }

  public static I18nMessage noMatchingMethodsOnObjectReturning(Object object, Class<?> returnType) {
    return factory.createMessage(BUNDLE_PATH, 220, StringMessageUtils.toString(object), StringMessageUtils.toString(returnType));
  }

  public static I18nMessage noEntryPointFoundForNoArgsMethod(final Object component, final String methodName) {
    return factory.createMessage(BUNDLE_PATH, 224, component, methodName);
  }

  public static I18nMessage failedToInvokeLifecycle(String phaseName, Object object) {
    return factory.createMessage(BUNDLE_PATH, 228, phaseName, object);
  }

  /**
   * Returns a message that is a product informatin.
   * 
   * @return message
   */
  public static I18nMessage productInformation() {
    String notset = CoreMessages.notSet().getMessage();
    return factory.createMessage(BUNDLE_PATH, 236, defaultString(MuleManifest.getProductDescription(), notset),
                                 defaultString(MuleManifest.getProductVersion(), notset),
                                 defaultString(MuleManifest.getVendorName(), notset) + " "
                                     + defaultString(MuleManifest.getVendorUrl(), notset));
  }

  public static I18nMessage noTransformerFoundForMessage(DataType input, DataType output) {
    return factory.createMessage(BUNDLE_PATH, 237, input, output);
  }

  public static I18nMessage errorReadingStream() {
    return factory.createMessage(BUNDLE_PATH, 238);
  }

  public static I18nMessage noEntryPointFoundForNoArgsMethodUsingResolver(final Object component, final String methodName) {
    return factory.createMessage(BUNDLE_PATH, 239, methodName, component);
  }

  public static I18nMessage noEntryPointFoundWithArgsUsingResolver(Object object, Object args) {
    return factory.createMessage(BUNDLE_PATH, 240, StringMessageUtils.toString(object), StringMessageUtils.toString(args));
  }

  public static I18nMessage noMatchingMethodsOnObjectReturningUsingResolver(Object object, Class<?> returnType) {
    return factory.createMessage(BUNDLE_PATH, 241, StringMessageUtils.toString(object), returnType.getClass().getName());
  }

  public static I18nMessage tooManyAcceptableMethodsOnObjectUsingResolverForTypes(Object object, Object types, String methods) {
    return factory.createMessage(BUNDLE_PATH, 242, StringMessageUtils.toString(object), StringMessageUtils.toString(types),
                                 methods);
  }

  public static I18nMessage tooManyMatchingMethodsOnObjectUsingResolverWhichReturn(Object object, Object returnType) {
    return factory.createMessage(BUNDLE_PATH, 243, StringMessageUtils.toString(returnType), StringMessageUtils.toString(object));
  }

  public static I18nMessage objectDoesNotImplementInterface(Object object, Class<?> interfaceClass) {
    return factory.createMessage(BUNDLE_PATH, 244, StringMessageUtils.toString(object), interfaceClass);
  }

  public static I18nMessage invocationSuccessfulCantSetError() {
    return factory.createMessage(BUNDLE_PATH, 245);
  }

  public static I18nMessage noMatchingMethodsOnObjectCalledUsingResolver(Object object, String methodName) {
    return factory.createMessage(BUNDLE_PATH, 246, StringMessageUtils.toString(object), methodName);
  }

  public static I18nMessage noJtaTransactionAvailable(final Thread callingThread) {
    return factory.createMessage(BUNDLE_PATH, 247, defaultString(callingThread.toString()));
  }

  public static I18nMessage notMuleXaTransaction(Object tx) {
    return factory.createMessage(BUNDLE_PATH, 248, tx.getClass());
  }

  public static I18nMessage failedToProcessExtractorFunction(String name) {
    return factory.createMessage(BUNDLE_PATH, 250, name);
  }

  public static I18nMessage objectAlreadyExists(String key) {
    return factory.createMessage(BUNDLE_PATH, 252, key);
  }

  public static I18nMessage noMuleTransactionAvailable() {
    return factory.createMessage(BUNDLE_PATH, 253);
  }

  public static I18nMessage objectAlreadyRegistered(String name, Object origObject, Object newObject) {
    return factory.createMessage(BUNDLE_PATH, 254, name, origObject + "." + origObject.getClass(),
                                 newObject + "." + newObject.getClass());
  }

  public static I18nMessage transformerNotImplementDiscoverable(Transformer transformer) {
    return transformerNotImplementDiscoverable(transformer.toString());
  }

  public static I18nMessage transformerNotImplementDiscoverable(Class<? extends Transformer> transformer) {
    return transformerNotImplementDiscoverable(transformer.getName());
  }

  private static I18nMessage transformerNotImplementDiscoverable(String transformer) {
    return factory.createMessage(BUNDLE_PATH, 255, transformer);
  }

  public static I18nMessage transformHasMultipleMatches(Class<?> input, Class<?> output,
                                                        List<? extends Transformer> transformers) {
    return factory.createMessage(BUNDLE_PATH, 256, input, output, StringMessageUtils.toString(transformers));
  }

  public static I18nMessage configurationBuilderSuccess(ConfigurationBuilder configurationBuilder, int numResources) {
    return factory.createMessage(BUNDLE_PATH, 257, configurationBuilder.getClass().getName(), new Integer(numResources));
  }

  public static I18nMessage configurationBuilderSuccess(ConfigurationBuilder configurationBuilder, String resources) {
    return factory.createMessage(BUNDLE_PATH, 258, configurationBuilder.getClass().getName(), resources);
  }

  public static I18nMessage configurationBuilderNoMatching(String resource) {
    return factory.createMessage(BUNDLE_PATH, 259, resource);
  }

  public static I18nMessage configurationBuilderError(ConfigurationBuilder configurationBuilder) {
    return factory.createMessage(BUNDLE_PATH, 260, StringMessageUtils.toString(configurationBuilder.getClass()));
  }

  public static I18nMessage nestedRetry() {
    return factory.createMessage(BUNDLE_PATH, 261);
  }

  public static I18nMessage expressionReturnedNull(String expr) {
    return factory.createMessage(BUNDLE_PATH, 263, expr);
  }

  public static I18nMessage expressionInvalidForProperty(String property, String expr) {
    return factory.createMessage(BUNDLE_PATH, 264, property, expr);
  }

  public static I18nMessage expressionMalformed(String expr, String eval) {
    return factory.createMessage(BUNDLE_PATH, 265, expr, eval);
  }

  public static I18nMessage correlationTimedOut(Object groupId) {
    return factory.createMessage(BUNDLE_PATH, 266, groupId);
  }

  public static I18nMessage transformerInvalidReturnType(Class<?> clazz, String transformerName) {
    return factory.createMessage(BUNDLE_PATH, 267, clazz, transformerName);
  }

  public static I18nMessage transactionFactoryIsMandatory(String action) {
    return factory.createMessage(BUNDLE_PATH, 269, action);
  }

  public static I18nMessage authorizationAttemptFailed() {
    return factory.createMessage(BUNDLE_PATH, 271);
  }

  public static I18nMessage retryPolicyExhausted(RetryPolicyTemplate policy) {
    return factory.createMessage(BUNDLE_PATH, 272, policy);
  }

  public static I18nMessage notConnectedYet(String what) {
    return factory.createMessage(BUNDLE_PATH, 273, what);
  }

  public static I18nMessage expressionResultWasNull(String expression) {
    return factory.createMessage(BUNDLE_PATH, 276, expression);
  }

  public static I18nMessage propertyDoesNotExistOnObject(String property, Object object) {
    return factory.createMessage(BUNDLE_PATH, 277, property, object);
  }

  public static I18nMessage commitTxButNoResource(Transaction tx) {
    return factory.createMessage(BUNDLE_PATH, 300, tx);
  }

  public static I18nMessage rollbackTxButNoResource(Transaction tx) {
    return factory.createMessage(BUNDLE_PATH, 301, tx);
  }

  public static I18nMessage cannotCopyStreamPayload(String streamType) {
    return factory.createMessage(BUNDLE_PATH, 302, streamType);
  }

  public static I18nMessage propertiesOrNotSet(String name, String properties) {
    return factory.createMessage(BUNDLE_PATH, 303, name, properties);
  }

  public static I18nMessage transformerMapBeanClassNotSet() {
    return factory.createMessage(BUNDLE_PATH, 304);
  }

  public static I18nMessage lifecyclePhaseNotRecognised(String phase) {
    return factory.createMessage(BUNDLE_PATH, 305, phase);
  }

  public static I18nMessage notificationListenerSubscriptionAlreadyRegistered(ListenerSubscriptionPair listenerPair) {
    return factory.createMessage(BUNDLE_PATH, 306, listenerPair);
  }

  public static I18nMessage applicationShutdownNormally(String appName, Date date) {
    return factory.createMessage(BUNDLE_PATH, 307, appName, date);
  }

  public static I18nMessage applicationWasUpForDuration(long duration) {
    String formattedDuration = DateUtils.getFormattedDuration(duration);
    return factory.createMessage(BUNDLE_PATH, 308, formattedDuration);
  }

  public static I18nMessage errorSchedulingMessageProcessorForAsyncInvocation(Processor processor) {
    return factory.createMessage(BUNDLE_PATH, 309, processor);
  }

  public static I18nMessage errorInvokingMessageProcessorAsynchronously(Processor processor) {
    return factory.createMessage(BUNDLE_PATH, 310, processor);
  }

  public static I18nMessage errorInvokingMessageProcessorWithinTransaction(Processor processor,
                                                                           TransactionConfig transactionConfig) {
    return factory.createMessage(BUNDLE_PATH, 311, processor, transactionConfig);
  }

  public static I18nMessage messageRejectedByFilter() {
    return factory.createMessage(BUNDLE_PATH, 314);
  }

  public static I18nMessage interruptedWaitingForPaused(String name) {
    return factory.createMessage(BUNDLE_PATH, 315, name);
  }

  public static I18nMessage objectHasMoreThanOnePostConstructAnnotation(Class<?> clazz) {
    return factory.createMessage(BUNDLE_PATH, 316, clazz.getName());
  }

  public static I18nMessage objectHasMoreThanOnePreDestroyAnnotation(Class<?> clazz) {
    return factory.createMessage(BUNDLE_PATH, 317, clazz.getName());
  }

  public static I18nMessage lifecycleMethodNotVoidOrHasParams(Method method) {
    return factory.createMessage(BUNDLE_PATH, 318, method.getName());
  }

  public static I18nMessage lifecycleMethodCannotBeStatic(Method method) {
    return factory.createMessage(BUNDLE_PATH, 319, method.getName());
  }

  public static I18nMessage lifecycleMethodCannotThrowChecked(Method method) {
    return factory.createMessage(BUNDLE_PATH, 320, method.getName());
  }

  public static I18nMessage cannotRenameInboundScopeProperty(String fromKey, String toKey) {
    return factory.createMessage(BUNDLE_PATH, 321, fromKey, toKey);
  }

  public static I18nMessage failedToFindEntrypointForComponent(String message) {
    return factory.createMessage(BUNDLE_PATH, 322, message);
  }

  public static I18nMessage illegalMIMEType(String badMIMIEType) {
    return factory.createMessage(BUNDLE_PATH, 324, badMIMIEType);
  }

  public static I18nMessage unexpectedMIMEType(String badMIMIEType, String goodMIMEType) {
    return factory.createMessage(BUNDLE_PATH, 325, badMIMIEType, goodMIMEType);
  }

  public static I18nMessage asyncDoesNotSupportTransactions() {
    return factory.createMessage(BUNDLE_PATH, 328);
  }

  public static I18nMessage methodWithNumParamsNotFoundOnObject(String method, int numArgments, Object object) {
    return factory.createMessage(BUNDLE_PATH, 329, method, numArgments, StringMessageUtils.toString(object));
  }

  public static I18nMessage expressionEnricherNotRegistered(String key) {
    return factory.createMessage(BUNDLE_PATH, 330, key);
  }

  public static I18nMessage authorizationDeniedOnEndpoint(String originationgConnectorName) {
    return factory.createMessage(BUNDLE_PATH, 331, originationgConnectorName);
  }

  public static I18nMessage objectStoreNotFound(String name) {
    return factory.createMessage(BUNDLE_PATH, 332, name);
  }

  public static I18nMessage propertyNotSerializableWasDropped(String key) {
    return factory.createMessage(BUNDLE_PATH, 333, key);
  }

  public static I18nMessage sessionPropertyNotSerializableWarning(String key) {
    return factory.createMessage(BUNDLE_PATH, 334, key);
  }

  public static I18nMessage expressionEvaluationFailed(String errorMessage, String expression) {
    return factory.createMessage(BUNDLE_PATH, 335, errorMessage, expression);
  }

  public static I18nMessage expressionFinalVariableCannotBeAssignedValue(String key) {
    return factory.createMessage(BUNDLE_PATH, 336, key);
  }

  public static I18nMessage inboundMessagePropertiesImmutable(Object key) {
    return factory.createMessage(BUNDLE_PATH, 337, key);
  }

  public static I18nMessage inboundMessagePropertiesImmutable() {
    return factory.createMessage(BUNDLE_PATH, 338);
  }

  public static I18nMessage inboundMessageAttachmentsImmutable(Object key) {
    return factory.createMessage(BUNDLE_PATH, 339, key);
  }

  public static I18nMessage inboundMessageAttachmentsImmutable() {
    return factory.createMessage(BUNDLE_PATH, 340);
  }

  public static I18nMessage invalidJdk(String jdkVersion, String validJdks) {
    return factory.createMessage(BUNDLE_PATH, 341, jdkVersion, validJdks);
  }

  public static I18nMessage servicesDeprecated() {
    return factory.createMessage(BUNDLE_PATH, 342);
  }

  public static I18nMessage modelDeprecated() {
    return factory.createMessage(BUNDLE_PATH, 343);
  }

  public static I18nMessage watermarkRequiresSynchronousProcessing() {
    return factory.createMessage(BUNDLE_PATH, 345);
  }

  public static I18nMessage couldNotRegisterNewScheduler(String schedulerName) {
    return factory.createMessage(BUNDLE_PATH, 346, schedulerName);
  }

  public static I18nMessage pollSourceReturnedNull(String flowName) {
    return factory.createMessage(BUNDLE_PATH, 347, flowName);
  }

  public static I18nMessage wrongMessageSource(String endpoint) {
    return factory.createMessage(BUNDLE_PATH, 348, endpoint);
  }

  public static I18nMessage notSerializableWatermark(String variableName) {
    return factory.createMessage(BUNDLE_PATH, 349, variableName);
  }

  public static I18nMessage nullWatermark() {
    return factory.createMessage(BUNDLE_PATH, 350);
  }
}
