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
 * A representation of the model object '<em><b>Environment Properties Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getProperty <em>Property</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getFactoryProperty <em>Factory Property</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getSystemProperty <em>System Property</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getMap <em>Map</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getList <em>List</em>}</li>
 *   <li>{@link org.mule.schema.EnvironmentPropertiesType#getFileProperties <em>File Properties</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType()
 * @model extendedMetaData="name='environment-propertiesType' kind='mixed'"
 * @generated
 */
public interface EnvironmentPropertiesType extends EObject {
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_Mixed()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_Group()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_Property()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_FactoryProperty()
     * @model type="org.mule.schema.FactoryPropertyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='factory-property' namespace='##targetNamespace' group='group:1'"
     * @generated
     */
    EList getFactoryProperty();

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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_SystemProperty()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_Map()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_List()
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
     * @see org.mule.schema.MulePackage#getEnvironmentPropertiesType_FileProperties()
     * @model type="org.mule.schema.FilePropertiesType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='file-properties' namespace='##targetNamespace' group='group:1'"
     * @generated
     */
    EList getFileProperties();

} // EnvironmentPropertiesType
