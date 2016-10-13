package dyvil.tools.gensrc;

import java.io.PrintStream;
import java.util.Iterator;

public class Specializer
{
	public static void processLines(Iterable<String> lines, PrintStream writer, ReplacementMap replacements)
	{
		processLines(lines.iterator(), writer, new LazyReplacementMap(replacements), false, true, true);
	}

	private static void processLines(Iterator<String> iterator, PrintStream writer, LazyReplacementMap replacements,
		                                boolean ifStatement, boolean outerCondition, boolean thisCondition)
	{
		boolean hasElse = false;

		while (iterator.hasNext())
		{
			final String line = iterator.next();
			final int length = line.length();

			final int hashIndex;
			if (length < 2 || (hashIndex = skipWhitespace(line, 0, length)) >= length || line.charAt(hashIndex) != '#')
			{
				// no leading directive

				if (outerCondition && thisCondition)
				{
					writer.println(processLine(line, replacements));
				}
				continue;
			}

			final int directiveStart = hashIndex + 1; // TODO ignore whitespace between # and identifier?
			final int directiveEnd = findIdentifierEnd(line, directiveStart, length);
			final String directive = line.substring(directiveStart, directiveEnd);
			switch (directive)
			{
			case "if":
				// nested if
				final int conditionStart = skipWhitespace(line, directiveEnd, length);
				final int conditionEnd = findIdentifierEnd(line, conditionStart, length);
				final String conditionString = line.substring(conditionStart, conditionEnd);
				final boolean condition = replacements.getBoolean(conditionString);

				processLines(iterator, writer, new LazyReplacementMap(replacements), true,
				             outerCondition && thisCondition, condition);
				continue;
			case "else":
				if (ifStatement && !hasElse)
				{
					thisCondition = !thisCondition;
					hasElse = true;
				}
				continue;
			case "endif":
				if (ifStatement)
				{
					return;
				}
				continue;
			case "process":
				if (outerCondition && thisCondition)
				{
					// process the remainder of the line
					final String remainder = line.substring(skipWhitespace(line, directiveEnd, length));
					writer.println(processLine(remainder, replacements));
				}
				continue;
			case "literal":
				if (outerCondition && thisCondition)
				{
					// simply append the remainder of the line verbatim
					final String remainder = line.substring(skipWhitespace(line, directiveEnd, length));
					writer.println(remainder);
				}
				continue;
			case "comment":
				continue;
			case "define":
				if (outerCondition && thisCondition)
				{
					final int keyStart = skipWhitespace(line, directiveEnd, length);
					final int keyEnd = findIdentifierEnd(line, keyStart, length);
					final int valueStart = skipWhitespace(line, keyEnd, length);

					final String key = line.substring(keyStart, keyEnd);
					final String value = line.substring(valueStart, length);
					replacements.define(key, value);
				}
				continue;
			case "undef":
			case "undefine":
				if (outerCondition && thisCondition)
				{
					final int keyStart = skipWhitespace(line, directiveEnd, length);
					final int keyEnd = findIdentifierEnd(line, keyStart, length);

					final String key = line.substring(keyStart, keyEnd);
					replacements.undefine(key);
				}
				continue;
			}

			// TODO invalid directive error/warning
		}
	}

	private static String processLine(String line, ReplacementMap replacements)
	{
		final int length = line.length();
		if (length == 0)
		{
			return line;
		}

		final StringBuilder builder = new StringBuilder(length);

		int prev = 0;

		for (int i = 0; i < length; )
		{
			final char c = line.charAt(i);

			if (c == '#' && i + 1 < length && line.charAt(i + 1) == '#')
			{
				// two consecutive ## are stripped

				// append contents before this identifier
				builder.append(line, prev, i);
				i = prev = i + 2; // advance by two characters
				continue;
			}
			if (!Character.isJavaIdentifierStart(c))
			{
				// advance to the first identifier start character
				i++;
				continue;
			}

			// append contents before this identifier
			builder.append(line, prev, i);

			// index of the first character that is not part of this identifier
			final int nextIndex = findIdentifierEnd(line, i + 1, length);
			final String key = line.substring(i, nextIndex);
			final String replacement = replacements.getReplacement(key);

			if (replacement != null)
			{
				builder.append(replacement); // append the replacement instead of the identifier
			}
			else
			{
				builder.append(line, i, nextIndex); // append the original identifier
			}
			i = prev = nextIndex;
		}

		// append remaining characters on line
		builder.append(line, prev, length);
		return builder.toString();
	}

	private static int findIdentifierEnd(String line, int start, int end)
	{
		for (; start < end; start++)
		{
			if (!Character.isJavaIdentifierPart(line.charAt(start)))
			{
				return start;
			}
		}
		return end;
	}

	/**
	 * Returns the first index greater than or equal to {@code start} where the character in {@code line} is NOT
	 * whitespace. If no such index is found, {@code end} is returned.
	 *
	 * @param line
	 * 	the string to check
	 * @param start
	 * 	the first index (inclusive) to check
	 * @param end
	 * 	the last index (exclusive) to check
	 *
	 * @return the first index {@code >= start} and {@code < end} where the character in the {@code string} is
	 * non-whitespace, or {@code end}.
	 */
	private static int skipWhitespace(String line, int start, int end)
	{
		for (; start < end; start++)
		{
			if (!Character.isWhitespace(line.charAt(start)))
			{
				return start;
			}
		}
		return end;
	}
}
