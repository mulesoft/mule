/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Model Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ModelType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getDescription <em>Description</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getEntryPointResolver <em>Entry Point Resolver</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getComponentFactory <em>Component Factory</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getComponentLifecycleAdapterFactory <em>Component Lifecycle Adapter Factory</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getComponentPoolFactory <em>Component Pool Factory</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getMuleDescriptor <em>Mule Descriptor</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.ModelType#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getModelType()
 * @model extendedMetaData="name='modelType' kind='mixed'"
 * @generated
 */
public interface ModelType extends EObject {
	/**
	 * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mixed</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getModelType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Description</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.mule.schema.MulePackage#getModelType_Description()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='description' namespace='##targetNamespace'"
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Entry Point Resolver</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Entry Point Resolver</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entry Point Resolver</em>' containment reference.
	 * @see #setEntryPointResolver(EntryPointResolverType)
	 * @see org.mule.schema.MulePackage#getModelType_EntryPointResolver()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='entry-point-resolver' namespace='##targetNamespace'"
	 * @generated
	 */
	EntryPointResolverType getEntryPointResolver();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getEntryPointResolver <em>Entry Point Resolver</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Entry Point Resolver</em>' containment reference.
	 * @see #getEntryPointResolver()
	 * @generated
	 */
	void setEntryPointResolver(EntryPointResolverType value);

	/**
	 * Returns the value of the '<em><b>Component Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Component Factory</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Component Factory</em>' containment reference.
	 * @see #setComponentFactory(ComponentFactoryType)
	 * @see org.mule.schema.MulePackage#getModelType_ComponentFactory()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='component-factory' namespace='##targetNamespace'"
	 * @generated
	 */
	ComponentFactoryType getComponentFactory();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getComponentFactory <em>Component Factory</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Component Factory</em>' containment reference.
	 * @see #getComponentFactory()
	 * @generated
	 */
	void setComponentFactory(ComponentFactoryType value);

	/**
	 * Returns the value of the '<em><b>Component Lifecycle Adapter Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Component Lifecycle Adapter Factory</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Component Lifecycle Adapter Factory</em>' containment reference.
	 * @see #setComponentLifecycleAdapterFactory(ComponentLifecycleAdapterFactoryType)
	 * @see org.mule.schema.MulePackage#getModelType_ComponentLifecycleAdapterFactory()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='component-lifecycle-adapter-factory' namespace='##targetNamespace'"
	 * @generated
	 */
	ComponentLifecycleAdapterFactoryType getComponentLifecycleAdapterFactory();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getComponentLifecycleAdapterFactory <em>Component Lifecycle Adapter Factory</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Component Lifecycle Adapter Factory</em>' containment reference.
	 * @see #getComponentLifecycleAdapterFactory()
	 * @generated
	 */
	void setComponentLifecycleAdapterFactory(ComponentLifecycleAdapterFactoryType value);

	/**
	 * Returns the value of the '<em><b>Component Pool Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Component Pool Factory</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Component Pool Factory</em>' containment reference.
	 * @see #setComponentPoolFactory(ComponentPoolFactoryType)
	 * @see org.mule.schema.MulePackage#getModelType_ComponentPoolFactory()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='component-pool-factory' namespace='##targetNamespace'"
	 * @generated
	 */
	ComponentPoolFactoryType getComponentPoolFactory();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getComponentPoolFactory <em>Component Pool Factory</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Component Pool Factory</em>' containment reference.
	 * @see #getComponentPoolFactory()
	 * @generated
	 */
	void setComponentPoolFactory(ComponentPoolFactoryType value);

	/**
	 * Returns the value of the '<em><b>Exception Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exception Strategy</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exception Strategy</em>' containment reference.
	 * @see #setExceptionStrategy(ExceptionStrategyType)
	 * @see org.mule.schema.MulePackage#getModelType_ExceptionStrategy()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='exception-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	ExceptionStrategyType getExceptionStrategy();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getExceptionStrategy <em>Exception Strategy</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exception Strategy</em>' containment reference.
	 * @see #getExceptionStrategy()
	 * @generated
	 */
	void setExceptionStrategy(ExceptionStrategyType value);

	/**
	 * Returns the value of the '<em><b>Mule Descriptor</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.MuleDescriptorType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mule Descriptor</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mule Descriptor</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getModelType_MuleDescriptor()
	 * @model type="org.mule.schema.MuleDescriptorType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='mule-descriptor' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getMuleDescriptor();

	/**
	 * Returns the value of the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Class Name</em>' attribute.
	 * @see #setClassName(String)
	 * @see org.mule.schema.MulePackage#getModelType_ClassName()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='className' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClassName();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getClassName <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Name</em>' attribute.
	 * @see #getClassName()
	 * @generated
	 */
	void setClassName(String value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.mule.schema.MulePackage#getModelType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ref</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ref</em>' attribute.
	 * @see #setRef(String)
	 * @see org.mule.schema.MulePackage#getModelType_Ref()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRef();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getRef <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ref</em>' attribute.
	 * @see #getRef()
	 * @generated
	 */
	void setRef(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The default value is <code>"seda"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Type</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #setType(String)
	 * @see org.mule.schema.MulePackage#getModelType_Type()
	 * @model default="seda" unique="false" unsettable="true" dataType="org.mule.schema.TypeType"
	 *        extendedMetaData="kind='attribute' name='type' namespace='##targetNamespace'"
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link org.mule.schema.ModelType#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #isSetType()
	 * @see #unsetType()
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.ModelType#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	void unsetType();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.ModelType#getType <em>Type</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Type</em>' attribute is set.
	 * @see #unsetType()
	 * @see #getType()
	 * @see #setType(String)
	 * @generated
	 */
	boolean isSetType();

} // ModelType
