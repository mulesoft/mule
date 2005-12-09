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

import org.mule.schema.EnableCorrelationType;
import org.mule.schema.FilterType;
import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.ReplyToType;
import org.mule.schema.RouterType;
import org.mule.schema.TransactionType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Router Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getGlobalEndpoint <em>Global Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getReplyTo <em>Reply To</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getEnableCorrelation <em>Enable Correlation</em>}</li>
 *   <li>{@link org.mule.schema.impl.RouterTypeImpl#getPropertyExtractor <em>Property Extractor</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RouterTypeImpl extends EObjectImpl implements RouterType {
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
	 * The default value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassName()
	 * @generated
	 * @ordered
	 */
	protected static final String CLASS_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassName()
	 * @generated
	 * @ordered
	 */
	protected String className = CLASS_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getEnableCorrelation() <em>Enable Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnableCorrelation()
	 * @generated
	 * @ordered
	 */
	protected static final EnableCorrelationType ENABLE_CORRELATION_EDEFAULT = EnableCorrelationType.IF_NOT_SET_LITERAL;

	/**
	 * The cached value of the '{@link #getEnableCorrelation() <em>Enable Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnableCorrelation()
	 * @generated
	 * @ordered
	 */
	protected EnableCorrelationType enableCorrelation = ENABLE_CORRELATION_EDEFAULT;

	/**
	 * This is true if the Enable Correlation attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean enableCorrelationESet = false;

	/**
	 * The default value of the '{@link #getPropertyExtractor() <em>Property Extractor</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPropertyExtractor()
	 * @generated
	 * @ordered
	 */
	protected static final String PROPERTY_EXTRACTOR_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPropertyExtractor() <em>Property Extractor</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPropertyExtractor()
	 * @generated
	 * @ordered
	 */
	protected String propertyExtractor = PROPERTY_EXTRACTOR_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RouterTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getRouterType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.ROUTER_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getEndpoint() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getRouterType_Endpoint());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getGlobalEndpoint() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getRouterType_GlobalEndpoint());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReplyToType getReplyTo() {
		return (ReplyToType)getMixed().get(MulePackage.eINSTANCE.getRouterType_ReplyTo(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetReplyTo(ReplyToType newReplyTo, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getRouterType_ReplyTo(), newReplyTo, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setReplyTo(ReplyToType newReplyTo) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getRouterType_ReplyTo(), newReplyTo);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionType getTransaction() {
		return (TransactionType)getMixed().get(MulePackage.eINSTANCE.getRouterType_Transaction(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTransaction(TransactionType newTransaction, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getRouterType_Transaction(), newTransaction, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransaction(TransactionType newTransaction) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getRouterType_Transaction(), newTransaction);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilterType getFilter() {
		return (FilterType)getMixed().get(MulePackage.eINSTANCE.getRouterType_Filter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetFilter(FilterType newFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getRouterType_Filter(), newFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setFilter(FilterType newFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getRouterType_Filter(), newFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getRouterType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getRouterType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getRouterType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setClassName(String newClassName) {
		String oldClassName = className;
		className = newClassName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ROUTER_TYPE__CLASS_NAME, oldClassName, className));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EnableCorrelationType getEnableCorrelation() {
		return enableCorrelation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEnableCorrelation(EnableCorrelationType newEnableCorrelation) {
		EnableCorrelationType oldEnableCorrelation = enableCorrelation;
		enableCorrelation = newEnableCorrelation == null ? ENABLE_CORRELATION_EDEFAULT : newEnableCorrelation;
		boolean oldEnableCorrelationESet = enableCorrelationESet;
		enableCorrelationESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ROUTER_TYPE__ENABLE_CORRELATION, oldEnableCorrelation, enableCorrelation, !oldEnableCorrelationESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetEnableCorrelation() {
		EnableCorrelationType oldEnableCorrelation = enableCorrelation;
		boolean oldEnableCorrelationESet = enableCorrelationESet;
		enableCorrelation = ENABLE_CORRELATION_EDEFAULT;
		enableCorrelationESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.ROUTER_TYPE__ENABLE_CORRELATION, oldEnableCorrelation, ENABLE_CORRELATION_EDEFAULT, oldEnableCorrelationESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetEnableCorrelation() {
		return enableCorrelationESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPropertyExtractor() {
		return propertyExtractor;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPropertyExtractor(String newPropertyExtractor) {
		String oldPropertyExtractor = propertyExtractor;
		propertyExtractor = newPropertyExtractor;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ROUTER_TYPE__PROPERTY_EXTRACTOR, oldPropertyExtractor, propertyExtractor));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.ROUTER_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.ROUTER_TYPE__ENDPOINT:
					return ((InternalEList)getEndpoint()).basicRemove(otherEnd, msgs);
				case MulePackage.ROUTER_TYPE__GLOBAL_ENDPOINT:
					return ((InternalEList)getGlobalEndpoint()).basicRemove(otherEnd, msgs);
				case MulePackage.ROUTER_TYPE__REPLY_TO:
					return basicSetReplyTo(null, msgs);
				case MulePackage.ROUTER_TYPE__TRANSACTION:
					return basicSetTransaction(null, msgs);
				case MulePackage.ROUTER_TYPE__FILTER:
					return basicSetFilter(null, msgs);
				case MulePackage.ROUTER_TYPE__PROPERTIES:
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
			case MulePackage.ROUTER_TYPE__MIXED:
				return getMixed();
			case MulePackage.ROUTER_TYPE__ENDPOINT:
				return getEndpoint();
			case MulePackage.ROUTER_TYPE__GLOBAL_ENDPOINT:
				return getGlobalEndpoint();
			case MulePackage.ROUTER_TYPE__REPLY_TO:
				return getReplyTo();
			case MulePackage.ROUTER_TYPE__TRANSACTION:
				return getTransaction();
			case MulePackage.ROUTER_TYPE__FILTER:
				return getFilter();
			case MulePackage.ROUTER_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.ROUTER_TYPE__CLASS_NAME:
				return getClassName();
			case MulePackage.ROUTER_TYPE__ENABLE_CORRELATION:
				return getEnableCorrelation();
			case MulePackage.ROUTER_TYPE__PROPERTY_EXTRACTOR:
				return getPropertyExtractor();
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
			case MulePackage.ROUTER_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.ROUTER_TYPE__ENDPOINT:
				getEndpoint().clear();
				getEndpoint().addAll((Collection)newValue);
				return;
			case MulePackage.ROUTER_TYPE__GLOBAL_ENDPOINT:
				getGlobalEndpoint().clear();
				getGlobalEndpoint().addAll((Collection)newValue);
				return;
			case MulePackage.ROUTER_TYPE__REPLY_TO:
				setReplyTo((ReplyToType)newValue);
				return;
			case MulePackage.ROUTER_TYPE__TRANSACTION:
				setTransaction((TransactionType)newValue);
				return;
			case MulePackage.ROUTER_TYPE__FILTER:
				setFilter((FilterType)newValue);
				return;
			case MulePackage.ROUTER_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.ROUTER_TYPE__CLASS_NAME:
				setClassName((String)newValue);
				return;
			case MulePackage.ROUTER_TYPE__ENABLE_CORRELATION:
				setEnableCorrelation((EnableCorrelationType)newValue);
				return;
			case MulePackage.ROUTER_TYPE__PROPERTY_EXTRACTOR:
				setPropertyExtractor((String)newValue);
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
			case MulePackage.ROUTER_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.ROUTER_TYPE__ENDPOINT:
				getEndpoint().clear();
				return;
			case MulePackage.ROUTER_TYPE__GLOBAL_ENDPOINT:
				getGlobalEndpoint().clear();
				return;
			case MulePackage.ROUTER_TYPE__REPLY_TO:
				setReplyTo((ReplyToType)null);
				return;
			case MulePackage.ROUTER_TYPE__TRANSACTION:
				setTransaction((TransactionType)null);
				return;
			case MulePackage.ROUTER_TYPE__FILTER:
				setFilter((FilterType)null);
				return;
			case MulePackage.ROUTER_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.ROUTER_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
				return;
			case MulePackage.ROUTER_TYPE__ENABLE_CORRELATION:
				unsetEnableCorrelation();
				return;
			case MulePackage.ROUTER_TYPE__PROPERTY_EXTRACTOR:
				setPropertyExtractor(PROPERTY_EXTRACTOR_EDEFAULT);
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
			case MulePackage.ROUTER_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.ROUTER_TYPE__ENDPOINT:
				return !getEndpoint().isEmpty();
			case MulePackage.ROUTER_TYPE__GLOBAL_ENDPOINT:
				return !getGlobalEndpoint().isEmpty();
			case MulePackage.ROUTER_TYPE__REPLY_TO:
				return getReplyTo() != null;
			case MulePackage.ROUTER_TYPE__TRANSACTION:
				return getTransaction() != null;
			case MulePackage.ROUTER_TYPE__FILTER:
				return getFilter() != null;
			case MulePackage.ROUTER_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.ROUTER_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
			case MulePackage.ROUTER_TYPE__ENABLE_CORRELATION:
				return isSetEnableCorrelation();
			case MulePackage.ROUTER_TYPE__PROPERTY_EXTRACTOR:
				return PROPERTY_EXTRACTOR_EDEFAULT == null ? propertyExtractor != null : !PROPERTY_EXTRACTOR_EDEFAULT.equals(propertyExtractor);
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
		result.append(", className: ");
		result.append(className);
		result.append(", enableCorrelation: ");
		if (enableCorrelationESet) result.append(enableCorrelation); else result.append("<unset>");
		result.append(", propertyExtractor: ");
		result.append(propertyExtractor);
		result.append(')');
		return result.toString();
	}

} //RouterTypeImpl
