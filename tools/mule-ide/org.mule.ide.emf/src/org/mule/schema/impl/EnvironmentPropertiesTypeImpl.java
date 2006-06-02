/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.EnvironmentPropertiesType;
import org.mule.schema.MulePackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Environment Properties Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getProperty <em>Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getFactoryProperty <em>Factory Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getSystemProperty <em>System Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getMap <em>Map</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getList <em>List</em>}</li>
 *   <li>{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl#getFileProperties <em>File Properties</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EnvironmentPropertiesTypeImpl extends EObjectImpl implements EnvironmentPropertiesType {
	/**
	 * The cached value of the '{@link #getMixed() <em>Mixed</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMixed()
	 * @generated
	 * @ordered
	 */
	protected FeatureMap mixed = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EnvironmentPropertiesTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getEnvironmentPropertiesType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getGroup() {
		return (FeatureMap)((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_Group());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_Property());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getFactoryProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_FactoryProperty());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getSystemProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_SystemProperty());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getMap() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_Map());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getList() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_List());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getFileProperties() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getEnvironmentPropertiesType_FileProperties());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__GROUP:
					return ((InternalEList)getGroup()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__PROPERTY:
					return ((InternalEList)getProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY:
					return ((InternalEList)getFactoryProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY:
					return ((InternalEList)getSystemProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MAP:
					return ((InternalEList)getMap()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__LIST:
					return ((InternalEList)getList()).basicRemove(otherEnd, msgs);
				case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES:
					return ((InternalEList)getFileProperties()).basicRemove(otherEnd, msgs);
				default:
					return eDynamicInverseRemove(otherEnd, featureID, baseClass, msgs);
			}
		}
		return eBasicSetContainer(null, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object eGet(EStructuralFeature eFeature, boolean resolve) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return getMixed();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__GROUP:
				return getGroup();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__PROPERTY:
				return getProperty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY:
				return getFactoryProperty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY:
				return getSystemProperty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MAP:
				return getMap();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__LIST:
				return getList();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES:
				return getFileProperties();
		}
		return eDynamicGet(eFeature, resolve);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eSet(EStructuralFeature eFeature, Object newValue) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__GROUP:
				getGroup().clear();
				getGroup().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__PROPERTY:
				getProperty().clear();
				getProperty().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY:
				getFactoryProperty().clear();
				getFactoryProperty().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY:
				getSystemProperty().clear();
				getSystemProperty().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MAP:
				getMap().clear();
				getMap().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__LIST:
				getList().clear();
				getList().addAll((Collection)newValue);
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES:
				getFileProperties().clear();
				getFileProperties().addAll((Collection)newValue);
				return;
		}
		eDynamicSet(eFeature, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eUnset(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__GROUP:
				getGroup().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__PROPERTY:
				getProperty().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY:
				getFactoryProperty().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY:
				getSystemProperty().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MAP:
				getMap().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__LIST:
				getList().clear();
				return;
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES:
				getFileProperties().clear();
				return;
		}
		eDynamicUnset(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean eIsSet(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__GROUP:
				return !getGroup().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__PROPERTY:
				return !getProperty().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY:
				return !getFactoryProperty().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY:
				return !getSystemProperty().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__MAP:
				return !getMap().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__LIST:
				return !getList().isEmpty();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES:
				return !getFileProperties().isEmpty();
		}
		return eDynamicIsSet(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (mixed: ");
		result.append(mixed);
		result.append(')');
		return result.toString();
	}

} //EnvironmentPropertiesTypeImpl
