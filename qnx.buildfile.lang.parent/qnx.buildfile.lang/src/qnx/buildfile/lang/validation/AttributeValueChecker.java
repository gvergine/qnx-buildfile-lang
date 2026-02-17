package qnx.buildfile.lang.validation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

public class AttributeValueChecker {

	private final static Map<String, Method> checkMethods = new HashMap<>();

	static
	{
		for (Method method : AttributeValueChecker.class.getDeclaredMethods()) {
			if (method.getName().startsWith("check_")
					&& Modifier.isStatic(method.getModifiers())) {
				checkMethods.put(method.getName(), method);
			}
		}
	}

	public static void check(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		final String name = valuedAttribute.getName();
		if (name == null) return;

		final String methodName = "check_" + name;

		try
		{
			if (checkMethods.containsKey(methodName))
			{
				checkMethods.get(methodName).invoke(null, valuedAttribute, buildfileDSLValidator);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/* Private Helpers*/
	private static boolean isValidUidOrGid(String s)
	{
		if (s == null) return false;
		if (s.equals("*")) return true;

		if (!s.matches("\\d+")) return false;

		try
		{
			long value = Long.parseLong(s);
			return value >= 0 && value <= Integer.MAX_VALUE;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}



	/**
	 * Checks if the value is a valid perms/dperms specification.
	 *
	 * @param value the attribute value
	 * @return true if the value is valid
	 */
	private static boolean isValidPerms(String value)
	{
		if (value == null) return false;

		// Wildcard — inherit from host
		if (value.equals("*")) return true;

		// Octal format: 3 or 4 octal digits (optionally starting with 0)
		if (value.matches("0?[0-7]{3}")) return true;

		// Symbolic mode: one or more comma-separated clauses
		// Each clause: [ugoa]*[+-=][rwxst]+
		// Examples: a+xr, u=rwx,g=rx,o=rx, +x, ug+rw
		return isValidSymbolicMode(value);
	}

	/**
	 * Validates a chmod-like symbolic mode string as used by QNX mkifs.
	 * <p>
	 * Format: one or more comma-separated clauses, each being {@code [ugoa]*[+-=][rwxst]+}
	 */
	private static boolean isValidSymbolicMode(String value)
	{
		if (value == null || value.isEmpty()) return false;

		String[] clauses = value.split(",", -1);
		for (String clause : clauses)
		{
			if (!clause.matches("[ugoa]*[+=\\-][rwxst]+"))
			{
				return false;
			}
		}
		return true;
	}

	/* Checks */
	public static void check_uid(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!isValidUidOrGid(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue() + "\" for uid",
					BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidUid");
		}	
	}

	public static void check_gid(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!isValidUidOrGid(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue() + "\" for gid",
					BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidGid");
		}
	}

	private final static List<String> AUTOSO_VALUES = Arrays.asList("n","none","l","list","a","add");
	public static void check_autoso(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!AUTOSO_VALUES.contains(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue() + "\" for autoso (n[one]|l[ist]|a[dd])",
					BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidAutoso");
		}
	}

	private final static List<String> COMPRESS_VALUES = Arrays.asList("1","2","3");
	public static void check_compress(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!COMPRESS_VALUES.contains(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue() + "\" for compress (1|2|3)",
					BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidCompress");
		}
	}

	private final static List<String> TYPE_VALUES = Arrays.asList("link","fifo","file","dir");
	public static void check_type(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!TYPE_VALUES.contains(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format\"" + valuedAttribute.getValue() + "\" for type {link|fifo|file|dir)",
					BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidType");
		}
	}


	public static void check_perms(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!isValidPerms(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue()
			+ "\" for perms (expected *, octal e.g. 0755, or symbolic e.g. a+rwx)",
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidPerms");
		}
	}


	public static void check_dperms(ValuedAttribute valuedAttribute, BaseDSLValidator buildfileDSLValidator)
	{
		if (!isValidPerms(valuedAttribute.getValue()))
		{
			buildfileDSLValidator.reportError("Wrong format \"" + valuedAttribute.getValue()
			+ "\" for dperms (expected *, octal e.g. 0755, or symbolic e.g. a+rwx)",
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE__VALUE,
					"invalidDperms");
		}
	}
}
