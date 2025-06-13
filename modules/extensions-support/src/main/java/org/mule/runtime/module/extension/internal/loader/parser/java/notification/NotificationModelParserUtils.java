/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.notification;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclarer;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.model.notification.ImmutableNotificationModel;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.extension.api.loader.parser.NotificationEmitterParser;
import org.mule.sdk.api.annotation.notification.NotificationActionProvider;
import org.mule.sdk.api.annotation.notification.NotificationActions;
import org.mule.sdk.api.notification.NotificationActionDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utilities for parsing Java defined {@link NotificationModel notification models}
 *
 * @since 4.5.0
 */
public final class NotificationModelParserUtils {

  public static List<NotificationModel> parseNotifications(AnnotationValueFetcher<NotificationActions> value,
                                                           String extensionNamespace,
                                                           ClassTypeLoader typeLoader) {
    Class<Enum> enumType = (Class<Enum>) value.getClassValue(NotificationActions::value).getDeclaringClass().orElse(null);
    if (enumType == null) {
      return new LinkedList<>();
    }

    return Stream.of((NotificationActionDefinition<?>[]) enumType.getEnumConstants())
        .map(action -> new ImmutableNotificationModel(extensionNamespace, ((Enum) action).name(),
                                                      typeLoader.load(action.getDataType().getType())))
        .collect(toList());
  }

  public static List<NotificationModel> parseLegacyNotifications(AnnotationValueFetcher<org.mule.runtime.extension.api.annotation.notification.NotificationActions> value,
                                                                 String extensionNamespace,
                                                                 ClassTypeLoader typeLoader) {
    Class<Enum> enumType =
        (Class<Enum>) value.getClassValue(org.mule.runtime.extension.api.annotation.notification.NotificationActions::value)
            .getDeclaringClass().orElse(null);
    if (enumType == null) {
      return new LinkedList<>();
    }

    return Stream.of(
                     (org.mule.runtime.extension.api.notification.NotificationActionDefinition<?>[]) enumType.getEnumConstants())
        .map(action -> new ImmutableNotificationModel(extensionNamespace, ((Enum) action).name(),
                                                      typeLoader.load(action.getDataType().getType())))
        .collect(toList());
  }

  public static List<String> getEmittedNotifications(WithAnnotations element,
                                                     String componentType,
                                                     String componentName) {
    return mapReduceSingleAnnotation(
                                     element,
                                     componentType,
                                     componentName,
                                     Fires.class,
                                     org.mule.sdk.api.annotation.notification.Fires.class,
                                     value -> getEmittedNotifications(value.getClassArrayValue(Fires::value)),
                                     value -> getEmittedNotifications(value
                                         .getClassArrayValue(org.mule.sdk.api.annotation.notification.Fires::value)))
                                             .orElse(emptyList());
  }

  public static void declareEmittedNotifications(NotificationEmitterParser parser,
                                                 ExecutableComponentDeclarer declarer,
                                                 Function<String, Optional<NotificationModel>> notificationMapper) {
    parser.getEmittedNotificationsStream(notificationMapper).forEach(declarer::withNotificationModel);
  }

  private static List<String> getEmittedNotifications(List<Type> types) {
    return types.stream()
        .filter(type -> type.getDeclaringClass().isPresent())
        .flatMap(type -> {
          Class<?> providerClass = type.getDeclaringClass().get();
          try {
            NotificationActionProvider provider = SdkNotificationActionProviderAdapter.from(providerClass.newInstance());
            return provider.getNotificationActions().stream().map(action -> ((Enum) action).name());
          } catch (InstantiationException | IllegalAccessException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not create NotificationActionProvider of type "
                + providerClass.getName()), e);
          }
        }).collect(toList());
  }

  private NotificationModelParserUtils() {}
}
