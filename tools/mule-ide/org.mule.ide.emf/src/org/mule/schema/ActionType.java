/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Action Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getActionType()
 * @model
 * @generated
 */
public final class ActionType extends AbstractEnumerator {
	/**
	 * The '<em><b>NONE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>NONE</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #NONE_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int NONE = 0;

	/**
	 * The '<em><b>ALWAYS BEGIN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>ALWAYS BEGIN</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #ALWAYS_BEGIN_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ALWAYS_BEGIN = 1;

	/**
	 * The '<em><b>BEGIN OR JOIN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>BEGIN OR JOIN</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #BEGIN_OR_JOIN_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int BEGIN_OR_JOIN = 2;

	/**
	 * The '<em><b>ALWAYS JOIN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>ALWAYS JOIN</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #ALWAYS_JOIN_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ALWAYS_JOIN = 3;

	/**
	 * The '<em><b>JOIN IF POSSIBLE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>JOIN IF POSSIBLE</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #JOIN_IF_POSSIBLE_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int JOIN_IF_POSSIBLE = 4;

	/**
	 * The '<em><b>NONE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #NONE
	 * @generated
	 * @ordered
	 */
	public static final ActionType NONE_LITERAL = new ActionType(NONE, "NONE");

	/**
	 * The '<em><b>ALWAYS BEGIN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ALWAYS_BEGIN
	 * @generated
	 * @ordered
	 */
	public static final ActionType ALWAYS_BEGIN_LITERAL = new ActionType(ALWAYS_BEGIN, "ALWAYS_BEGIN");

	/**
	 * The '<em><b>BEGIN OR JOIN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #BEGIN_OR_JOIN
	 * @generated
	 * @ordered
	 */
	public static final ActionType BEGIN_OR_JOIN_LITERAL = new ActionType(BEGIN_OR_JOIN, "BEGIN_OR_JOIN");

	/**
	 * The '<em><b>ALWAYS JOIN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ALWAYS_JOIN
	 * @generated
	 * @ordered
	 */
	public static final ActionType ALWAYS_JOIN_LITERAL = new ActionType(ALWAYS_JOIN, "ALWAYS_JOIN");

	/**
	 * The '<em><b>JOIN IF POSSIBLE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #JOIN_IF_POSSIBLE
	 * @generated
	 * @ordered
	 */
	public static final ActionType JOIN_IF_POSSIBLE_LITERAL = new ActionType(JOIN_IF_POSSIBLE, "JOIN_IF_POSSIBLE");

	/**
	 * An array of all the '<em><b>Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ActionType[] VALUES_ARRAY =
		new ActionType[] {
			NONE_LITERAL,
			ALWAYS_BEGIN_LITERAL,
			BEGIN_OR_JOIN_LITERAL,
			ALWAYS_JOIN_LITERAL,
			JOIN_IF_POSSIBLE_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Action Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ActionType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ActionType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Action Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ActionType get(int value) {
		switch (value) {
			case NONE: return NONE_LITERAL;
			case ALWAYS_BEGIN: return ALWAYS_BEGIN_LITERAL;
			case BEGIN_OR_JOIN: return BEGIN_OR_JOIN_LITERAL;
			case ALWAYS_JOIN: return ALWAYS_JOIN_LITERAL;
			case JOIN_IF_POSSIBLE: return JOIN_IF_POSSIBLE_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private ActionType(int value, String name) {
		super(value, name);
	}

} //ActionType
