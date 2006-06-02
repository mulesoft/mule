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
 * A representation of the literals of the enumeration '<em><b>Pool Exhausted Action Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getPoolExhaustedActionType()
 * @model
 * @generated
 */
public final class PoolExhaustedActionType extends AbstractEnumerator {
	/**
	 * The '<em><b>WAIT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>WAIT</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #WAIT_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int WAIT = 0;

	/**
	 * The '<em><b>DISCARD</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>DISCARD</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #DISCARD_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DISCARD = 1;

	/**
	 * The '<em><b>DISCARD OLDEST</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>DISCARD OLDEST</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #DISCARD_OLDEST_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DISCARD_OLDEST = 2;

	/**
	 * The '<em><b>ABORT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>ABORT</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #ABORT_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ABORT = 3;

	/**
	 * The '<em><b>RUN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>RUN</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #RUN_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RUN = 4;

	/**
	 * The '<em><b>WAIT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #WAIT
	 * @generated
	 * @ordered
	 */
	public static final PoolExhaustedActionType WAIT_LITERAL = new PoolExhaustedActionType(WAIT, "WAIT");

	/**
	 * The '<em><b>DISCARD</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DISCARD
	 * @generated
	 * @ordered
	 */
	public static final PoolExhaustedActionType DISCARD_LITERAL = new PoolExhaustedActionType(DISCARD, "DISCARD");

	/**
	 * The '<em><b>DISCARD OLDEST</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DISCARD_OLDEST
	 * @generated
	 * @ordered
	 */
	public static final PoolExhaustedActionType DISCARD_OLDEST_LITERAL = new PoolExhaustedActionType(DISCARD_OLDEST, "DISCARD_OLDEST");

	/**
	 * The '<em><b>ABORT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ABORT
	 * @generated
	 * @ordered
	 */
	public static final PoolExhaustedActionType ABORT_LITERAL = new PoolExhaustedActionType(ABORT, "ABORT");

	/**
	 * The '<em><b>RUN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RUN
	 * @generated
	 * @ordered
	 */
	public static final PoolExhaustedActionType RUN_LITERAL = new PoolExhaustedActionType(RUN, "RUN");

	/**
	 * An array of all the '<em><b>Pool Exhausted Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final PoolExhaustedActionType[] VALUES_ARRAY =
		new PoolExhaustedActionType[] {
			WAIT_LITERAL,
			DISCARD_LITERAL,
			DISCARD_OLDEST_LITERAL,
			ABORT_LITERAL,
			RUN_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Pool Exhausted Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Pool Exhausted Action Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static PoolExhaustedActionType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			PoolExhaustedActionType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Pool Exhausted Action Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static PoolExhaustedActionType get(int value) {
		switch (value) {
			case WAIT: return WAIT_LITERAL;
			case DISCARD: return DISCARD_LITERAL;
			case DISCARD_OLDEST: return DISCARD_OLDEST_LITERAL;
			case ABORT: return ABORT_LITERAL;
			case RUN: return RUN_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private PoolExhaustedActionType(int value, String name) {
		super(value, name);
	}

} //PoolExhaustedActionType
