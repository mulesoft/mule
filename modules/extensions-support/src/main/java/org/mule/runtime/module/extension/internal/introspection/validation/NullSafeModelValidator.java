/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;

/**
 * Validates that all fields of the {@link ParameterModel parameters} which are annotated with {@link NullSafe} honor that:
 * <ul>
 * <li>Both dictionaries and collections cannot specify a particular {@link NullSafe#defaultImplementingType()}</li>
 * <li>{@link NullSafe#defaultImplementingType()} must be assignable to the field</li>
 * <li>{@link NullSafe} cannot be used with basic types</li> reserved name.
 * </ul>
 * 
 * @since 4.0
 */
public final class NullSafeModelValidator implements ModelValidator {

  private TypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {

    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        model.getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            if (objectType.getMetadataFormat().equals(JAVA)) {
              objectType.getAnnotation(TypeIdAnnotation.class).map(TypeIdAnnotation::getValue)
                  .ifPresent(typeId -> typeLoader.load(typeId).ifPresent(fieldMetadataType -> objectType
                      .getFields().stream()
                      .filter(f -> f.getAnnotation(NullSafeTypeAnnotation.class).isPresent())
                      .forEach(f -> validateField(getLocalPart(f), f, getType(fieldMetadataType),
                                                  f.getAnnotation(NullSafeTypeAnnotation.class).get()))));
            }
          }

          private void validateField(String fieldName, ObjectFieldType field, Class<?> declaringClass,
                                     NullSafeTypeAnnotation nullSafeTypeAnnotation) {
            Class<?> nullSafeType = nullSafeTypeAnnotation.getType();
            Class<?> fieldType = getType(field.getValue());
            boolean hasDefaultOverride = nullSafeTypeAnnotation.hasDefaultOverride();
            field.getValue().accept(new BasicTypeMetadataVisitor() {

              @Override
              protected void visitBasicType(MetadataType metadataType) {
                throw new IllegalParameterModelDefinitionException(
                                                                   format("Field '%s' in class '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                                                                       + "used with complex types (Pojos, Lists, Maps)",
                                                                          fieldName, declaringClass.getName(),
                                                                          NullSafe.class.getSimpleName(),
                                                                          fieldType.getName()));
              }

              @Override
              public void visitArrayType(ArrayType arrayType) {
                if (hasDefaultOverride) {
                  throw new IllegalParameterModelDefinitionException(format("Field '%s' in class '%s' is annotated with '@%s' is of type '%s'"
                      + " but a 'defaultImplementingType' was provided."
                      + " Type override is not allowed for Collections",
                                                                            fieldName,
                                                                            declaringClass.getName(),
                                                                            NullSafe.class.getSimpleName(),
                                                                            fieldType.getName()));
                }
              }

              @Override
              public void visitObject(ObjectType objectType) {
                // TODO: Reenable these checks when MULE-10679 brings introspectionUtils to a visible scope
                // if (hasDefaultOverride && isInstantiable(parameter.getDeclaration().getType())) {
                // throw new IllegalParameterModelDefinitionException(
                // format("Field '%s' in class '%s' is annotated with '@%s' is of concrete type '%s',"
                // + " but a 'defaultImplementingType' was provided."
                // + " Type override is not allowed for concrete types",
                // field.getName(),
                // declaringClass.getName(),
                // NullSafe.class.getSimpleName(),
                // field.getType().getName()));
                // }
                //
                // if (!isInstantiable(nullSafeType)) {
                // throw new IllegalParameterModelDefinitionException(
                // format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                // + "used with complex instantiable types (Pojos, Lists, Maps)",
                // extensionParameter.getName(),
                // NullSafe.class.getSimpleName(),
                // extensionParameter.getType().getName()));
                // }

                if (hasDefaultOverride && !fieldType.isAssignableFrom((Class) nullSafeType)) {
                  throw new IllegalParameterModelDefinitionException(
                                                                     format("Field '%s' in class '%s' is annotated with '@%s' of type '%s', but provided type '%s"
                                                                         + " is not a subtype of the parameter's type",
                                                                            fieldName,
                                                                            declaringClass.getName(),
                                                                            NullSafe.class.getSimpleName(),
                                                                            fieldType.getName(),
                                                                            nullSafeType.getTypeName()));
                }
              }

              @Override
              public void visitDictionary(DictionaryType dictionaryType) {
                if (hasDefaultOverride) {
                  throw new IllegalParameterModelDefinitionException(format("Field '%s' in class '%s' is annotated with '@%s' is of type '%s'"
                      + " but a 'defaultImplementingType' was provided."
                      + " Type override is not allowed for Maps",
                                                                            fieldName,
                                                                            declaringClass.getName(),
                                                                            NullSafe.class.getSimpleName(),
                                                                            fieldType.getName()));
                }
              }
            });
          }
        });
      }
    }.walk(extensionModel);
  }
}
