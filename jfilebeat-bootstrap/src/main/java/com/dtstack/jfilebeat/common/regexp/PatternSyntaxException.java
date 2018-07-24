package com.dtstack.jfilebeat.common.regexp;

import sun.security.action.GetPropertyAction;

public class PatternSyntaxException
extends IllegalArgumentException
{
private static final long serialVersionUID = -3864639126226059218L;

private final String desc;
private final String pattern;
private final int index;

/**
 * Constructs a new instance of this class.
 *
 * @param  desc
 *         A description of the error
 *
 * @param  regex
 *         The erroneous pattern
 *
 * @param  index
 *         The approximate index in the pattern of the error,
 *         or <tt>-1</tt> if the index is not known
 */
public PatternSyntaxException(String desc, String regex, int index) {
    this.desc = desc;
    this.pattern = regex;
    this.index = index;
}

/**
 * Retrieves the error index.
 *
 * @return  The approximate index in the pattern of the error,
 *         or <tt>-1</tt> if the index is not known
 */
public int getIndex() {
    return index;
}

/**
 * Retrieves the description of the error.
 *
 * @return  The description of the error
 */
public String getDescription() {
    return desc;
}

/**
 * Retrieves the erroneous regular-expression pattern.
 *
 * @return  The erroneous pattern
 */
public String getPattern() {
    return pattern;
}

private static final String nl =
    java.security.AccessController
        .doPrivileged(new GetPropertyAction("line.separator"));

/**
 * Returns a multi-line string containing the description of the syntax
 * error and its index, the erroneous regular-expression pattern, and a
 * visual indication of the error index within the pattern.
 *
 * @return  The full detail message
 */
public String getMessage() {
    StringBuffer sb = new StringBuffer();
    sb.append(desc);
    if (index >= 0) {
        sb.append(" near index ");
        sb.append(index);
    }
    sb.append(nl);
    sb.append(pattern);
    if (index >= 0) {
        sb.append(nl);
        for (int i = 0; i < index; i++) sb.append(' ');
        sb.append('^');
    }
    return sb.toString();
}

}
