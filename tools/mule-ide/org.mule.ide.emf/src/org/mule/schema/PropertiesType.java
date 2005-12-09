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
 * A representation of the model object '<em><b>Properties Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.PropertiesType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getProperty <em>Property</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getFactoryProperty <em>Factory Property</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getContainerProperty <em>Container Property</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getSystemProperty <em>System Property</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getMap <em>Map</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getList <em>List</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getFileProperties <em>File Properties</em>}</li>
 *   <li>{@link org.mule.schema.PropertiesType#getTextProperty <em>Text Property</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getPropertiesType()
 * @model extendedMetaData="name='propertiesType' kind='mixed'"
 * @generated
 */
public interface PropertiesType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getPropertiesType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Group</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Group</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Group</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_Group()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='group' name='group:1'"
	 * @generated
	 */
	FeatureMap getGroup();

	/**
	 * Returns the value of the '<em><b>Property</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.PropertyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Property</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Property</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_Property()
	 * @model type="org.mule.schema.PropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='property' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getProperty();

	/**
	 * Returns the value of the '<em><b>Factory Property</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.FactoryPropertyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Factory Property</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Factory Property</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_FactoryProperty()
	 * @model type="org.mule.schema.FactoryPropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='factory-property' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getFactoryProperty();

	/**
	 * Returns the value of the '<em><b>Container Property</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ContainerPropertyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Container Property</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Container Property</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_ContainerProperty()
	 * @model type="org.mule.schema.ContainerPropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='container-property' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getContainerProperty();

	/**
	 * Returns the value of the '<em><b>System Property</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.SystemPropertyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>System Property</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>System Property</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_SystemProperty()
	 * @model type="org.mule.schema.SystemPropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='system-property' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getSystemProperty();

	/**
	 * Returns the value of the '<em><b>Map</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.MapType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Map</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Map</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_Map()
	 * @model type="org.mule.schema.MapType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='map' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getMap();

	/**
	 * Returns the value of the '<em><b>List</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ListType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>List</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>List</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_List()
	 * @model type="org.mule.schema.ListType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='list' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getList();

	/**
	 * Returns the value of the '<em><b>File Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.FilePropertiesType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>File Properties</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>File Properties</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_FileProperties()
	 * @model type="org.mule.schema.FilePropertiesType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='file-properties' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getFileProperties();

	/**
	 * Returns the value of the '<em><b>Text Property</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.TextPropertyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Text Property</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Text Property</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getPropertiesType_TextProperty()
	 * @model type="org.mule.schema.TextPropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='text-property' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getTextProperty();

} // PropertiesType
