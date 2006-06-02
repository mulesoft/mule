/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.MapType;
import org.mule.schema.MulePackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Map Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getProperty <em>Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getFactoryProperty <em>Factory Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getContainerProperty <em>Container Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getSystemProperty <em>System Property</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getMap <em>Map</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getList <em>List</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getFileProperties <em>File Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.MapTypeImpl#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MapTypeImpl extends EObjectImpl implements MapType {
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
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MapTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getMapType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.MAP_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getGroup() {
		return (FeatureMap)((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMapType_Group());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_Property());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getFactoryProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_FactoryProperty());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getContainerProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_ContainerProperty());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getSystemProperty() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_SystemProperty());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getMap() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_Map());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getList() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_List());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getFileProperties() {
		return ((FeatureMap)getGroup()).list(MulePackage.eINSTANCE.getMapType_FileProperties());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MAP_TYPE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.MAP_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__GROUP:
					return ((InternalEList)getGroup()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__PROPERTY:
					return ((InternalEList)getProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__FACTORY_PROPERTY:
					return ((InternalEList)getFactoryProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__CONTAINER_PROPERTY:
					return ((InternalEList)getContainerProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__SYSTEM_PROPERTY:
					return ((InternalEList)getSystemProperty()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__MAP:
					return ((InternalEList)getMap()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__LIST:
					return ((InternalEList)getList()).basicRemove(otherEnd, msgs);
				case MulePackage.MAP_TYPE__FILE_PROPERTIES:
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
			case MulePackage.MAP_TYPE__MIXED:
				return getMixed();
			case MulePackage.MAP_TYPE__GROUP:
				return getGroup();
			case MulePackage.MAP_TYPE__PROPERTY:
				return getProperty();
			case MulePackage.MAP_TYPE__FACTORY_PROPERTY:
				return getFactoryProperty();
			case MulePackage.MAP_TYPE__CONTAINER_PROPERTY:
				return getContainerProperty();
			case MulePackage.MAP_TYPE__SYSTEM_PROPERTY:
				return getSystemProperty();
			case MulePackage.MAP_TYPE__MAP:
				return getMap();
			case MulePackage.MAP_TYPE__LIST:
				return getList();
			case MulePackage.MAP_TYPE__FILE_PROPERTIES:
				return getFileProperties();
			case MulePackage.MAP_TYPE__NAME:
				return getName();
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
			case MulePackage.MAP_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__GROUP:
				getGroup().clear();
				getGroup().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__PROPERTY:
				getProperty().clear();
				getProperty().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__FACTORY_PROPERTY:
				getFactoryProperty().clear();
				getFactoryProperty().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__CONTAINER_PROPERTY:
				getContainerProperty().clear();
				getContainerProperty().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__SYSTEM_PROPERTY:
				getSystemProperty().clear();
				getSystemProperty().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__MAP:
				getMap().clear();
				getMap().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__LIST:
				getList().clear();
				getList().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__FILE_PROPERTIES:
				getFileProperties().clear();
				getFileProperties().addAll((Collection)newValue);
				return;
			case MulePackage.MAP_TYPE__NAME:
				setName((String)newValue);
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
			case MulePackage.MAP_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.MAP_TYPE__GROUP:
				getGroup().clear();
				return;
			case MulePackage.MAP_TYPE__PROPERTY:
				getProperty().clear();
				return;
			case MulePackage.MAP_TYPE__FACTORY_PROPERTY:
				getFactoryProperty().clear();
				return;
			case MulePackage.MAP_TYPE__CONTAINER_PROPERTY:
				getContainerProperty().clear();
				return;
			case MulePackage.MAP_TYPE__SYSTEM_PROPERTY:
				getSystemProperty().clear();
				return;
			case MulePackage.MAP_TYPE__MAP:
				getMap().clear();
				return;
			case MulePackage.MAP_TYPE__LIST:
				getList().clear();
				return;
			case MulePackage.MAP_TYPE__FILE_PROPERTIES:
				getFileProperties().clear();
				return;
			case MulePackage.MAP_TYPE__NAME:
				setName(NAME_EDEFAULT);
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
			case MulePackage.MAP_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.MAP_TYPE__GROUP:
				return !getGroup().isEmpty();
			case MulePackage.MAP_TYPE__PROPERTY:
				return !getProperty().isEmpty();
			case MulePackage.MAP_TYPE__FACTORY_PROPERTY:
				return !getFactoryProperty().isEmpty();
			case MulePackage.MAP_TYPE__CONTAINER_PROPERTY:
				return !getContainerProperty().isEmpty();
			case MulePackage.MAP_TYPE__SYSTEM_PROPERTY:
				return !getSystemProperty().isEmpty();
			case MulePackage.MAP_TYPE__MAP:
				return !getMap().isEmpty();
			case MulePackage.MAP_TYPE__LIST:
				return !getList().isEmpty();
			case MulePackage.MAP_TYPE__FILE_PROPERTIES:
				return !getFileProperties().isEmpty();
			case MulePackage.MAP_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
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
		result.append(", name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} //MapTypeImpl
