/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.INITIALIZE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsNamespace;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider;
import org.mule.runtime.extension.api.annotation.notification.NotificationActions;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.model.notification.ImmutableNotificationModel;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.Map;
import java.util.Optional;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel}, their {@link OperationModel} and
 * {@link SourceModel} from the used {@link NotificationActions} and {@link Fires} in an Annotation based extension.
 *
 * @since 4.1
 */
public class NotificationsDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return INITIALIZE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    Optional<ExtensionTypeDescriptorModelProperty> extensionType =
        declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    String extensionNamespace = getExtensionsNamespace(declaration);
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    if (extensionType.isPresent() && extensionType.get().getType().getDeclaringClass().isPresent()) {
      Type extensionElement = extensionType.get().getType();
      Optional<NotificationActions> annotation = extensionElement.getAnnotation(NotificationActions.class);
      annotation.ifPresent(actionsAnnotation -> {
        NotificationActionDefinition<?>[] actions =
            (NotificationActionDefinition<?>[]) actionsAnnotation.value().getEnumConstants();
        Map<NotificationActionDefinition, NotificationModel> notificationModels = new SmallMap<>();
        stream(actions).forEach(action -> {
          NotificationModel model = new ImmutableNotificationModel(extensionNamespace, ((Enum) action).name(),
                                                                   typeLoader.load(action.getDataType().getType()));
          declaration.addNotificationModel(model);
          notificationModels.put(action, model);
        });

        new IdempotentDeclarationWalker() {

          @Override
          public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
            Optional<ExtensionOperationDescriptorModelProperty> modelProperty =
                declaration.getModelProperty(ExtensionOperationDescriptorModelProperty.class);

            if (modelProperty.isPresent()) {
              MethodElement method = modelProperty.get().getOperationElement();
              Optional<Fires> emitsNotifications =
                  getOperationNotificationDeclaration(method, extensionElement);
              includeNotificationDeclarationIfNeeded(declaration, emitsNotifications);
            }
          }

          @Override
          public void onSource(SourceDeclaration declaration) {
            Optional<ExtensionTypeDescriptorModelProperty> modelProperty =
                declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);

            if (modelProperty.isPresent()) {
              Type sourceContainer = modelProperty.get().getType();
              Optional<Fires> emitsNotifications = getNotificationDeclaration(sourceContainer, extensionElement);
              includeNotificationDeclarationIfNeeded(declaration, emitsNotifications);
            }
          }

          private Optional<Fires> getOperationNotificationDeclaration(MethodElement operationMethod,
                                                                      Type extensionElement) {
            Type operationContainer = operationMethod.getEnclosingType();
            return ofNullable(operationMethod.getAnnotation(Fires.class))
                .orElse(getNotificationDeclaration(operationContainer, extensionElement));
          }

          private Optional<Fires> getNotificationDeclaration(Type container, Type extensionElement) {
            return ofNullable(container.getAnnotation(Fires.class)
                .orElseGet(() -> extensionElement.getAnnotation(Fires.class)
                    .orElse(null)));
          }

          private void includeNotificationDeclarationIfNeeded(ExecutableComponentDeclaration declaration,
                                                              Optional<Fires> emitsNotifications) {
            emitsNotifications.ifPresent(emits -> {
              Class<? extends NotificationActionProvider>[] providers = emits.value();
              stream(providers).forEach(provider -> {
                try {
                  NotificationActionProvider notificationActionProvider = provider.newInstance();
                  notificationActionProvider.getNotificationActions().stream()
                      .map(action -> validateEmits(actions, action))
                      .forEach(action -> declaration.addNotificationModel(notificationModels.get(action)));
                } catch (InstantiationException | IllegalAccessException e) {
                  throw new MuleRuntimeException(createStaticMessage("Could not create NotificationActionProvider of type "
                      + provider.getName()), e);
                }
              });
            });
          }

          private NotificationActionDefinition validateEmits(NotificationActionDefinition[] actions,
                                                             NotificationActionDefinition action) {
            Class<?> extensionAction = actions.getClass().getComponentType();

            if (!action.getClass().equals(extensionAction) && !action.getClass().getSuperclass().equals(extensionAction)) {
              throw new IllegalModelDefinitionException(format("Invalid EmitsNotification detected, the extension declared" +
                  " firing notifications of %s type, but a notification of %s type has been detected",
                                                               extensionAction, action.getClass()));
            } else {
              return action;
            }
          }

        }.walk(declaration);
      });

    }
  }

}
