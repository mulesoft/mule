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

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.QueueProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Queue Profile Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.QueueProfileTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.QueueProfileTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.QueueProfileTypeImpl#getMaxOutstandingMessages <em>Max Outstanding Messages</em>}</li>
 *   <li>{@link org.mule.schema.impl.QueueProfileTypeImpl#isPersistent <em>Persistent</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class QueueProfileTypeImpl extends EObjectImpl implements QueueProfileType {
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
	 * The default value of the '{@link #getMaxOutstandingMessages() <em>Max Outstanding Messages</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxOutstandingMessages()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_OUTSTANDING_MESSAGES_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxOutstandingMessages() <em>Max Outstanding Messages</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxOutstandingMessages()
	 * @generated
	 * @ordered
	 */
	protected String maxOutstandingMessages = MAX_OUTSTANDING_MESSAGES_EDEFAULT;

	/**
	 * The default value of the '{@link #isPersistent() <em>Persistent</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isPersistent()
	 * @generated
	 * @ordered
	 */
	protected static final boolean PERSISTENT_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isPersistent() <em>Persistent</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isPersistent()
	 * @generated
	 * @ordered
	 */
	protected boolean persistent = PERSISTENT_EDEFAULT;

	/**
	 * This is true if the Persistent attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean persistentESet = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected QueueProfileTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getQueueProfileType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.QUEUE_PROFILE_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getQueueProfileType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getQueueProfileType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getQueueProfileType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxOutstandingMessages() {
		return maxOutstandingMessages;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxOutstandingMessages(String newMaxOutstandingMessages) {
		String oldMaxOutstandingMessages = maxOutstandingMessages;
		maxOutstandingMessages = newMaxOutstandingMessages;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES, oldMaxOutstandingMessages, maxOutstandingMessages));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isPersistent() {
		return persistent;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPersistent(boolean newPersistent) {
		boolean oldPersistent = persistent;
		persistent = newPersistent;
		boolean oldPersistentESet = persistentESet;
		persistentESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT, oldPersistent, persistent, !oldPersistentESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetPersistent() {
		boolean oldPersistent = persistent;
		boolean oldPersistentESet = persistentESet;
		persistent = PERSISTENT_EDEFAULT;
		persistentESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT, oldPersistent, PERSISTENT_EDEFAULT, oldPersistentESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetPersistent() {
		return persistentESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.QUEUE_PROFILE_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.QUEUE_PROFILE_TYPE__PROPERTIES:
					return basicSetProperties(null, msgs);
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
			case MulePackage.QUEUE_PROFILE_TYPE__MIXED:
				return getMixed();
			case MulePackage.QUEUE_PROFILE_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES:
				return getMaxOutstandingMessages();
			case MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT:
				return isPersistent() ? Boolean.TRUE : Boolean.FALSE;
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
			case MulePackage.QUEUE_PROFILE_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES:
				setMaxOutstandingMessages((String)newValue);
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT:
				setPersistent(((Boolean)newValue).booleanValue());
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
			case MulePackage.QUEUE_PROFILE_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES:
				setMaxOutstandingMessages(MAX_OUTSTANDING_MESSAGES_EDEFAULT);
				return;
			case MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT:
				unsetPersistent();
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
			case MulePackage.QUEUE_PROFILE_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.QUEUE_PROFILE_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES:
				return MAX_OUTSTANDING_MESSAGES_EDEFAULT == null ? maxOutstandingMessages != null : !MAX_OUTSTANDING_MESSAGES_EDEFAULT.equals(maxOutstandingMessages);
			case MulePackage.QUEUE_PROFILE_TYPE__PERSISTENT:
				return isSetPersistent();
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
		result.append(", maxOutstandingMessages: ");
		result.append(maxOutstandingMessages);
		result.append(", persistent: ");
		if (persistentESet) result.append(persistent); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //QueueProfileTypeImpl
