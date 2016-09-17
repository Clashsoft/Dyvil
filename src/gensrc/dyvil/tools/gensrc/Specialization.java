package dyvil.tools.gensrc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class Specialization
{
	private Properties substitutions;

	private final File   sourceFile;
	private final String templateName;
	private final String name;

	public Specialization(File sourceFile, String templateName, String specName)
	{
		this.sourceFile = sourceFile;
		this.templateName = templateName;
		this.name = specName;
	}

	public File getSourceFile()
	{
		return this.sourceFile;
	}

	public String getTemplateName()
	{
		return this.templateName;
	}

	public String getName()
	{
		return this.name;
	}

	public String getFileName()
	{
		return this.getSubstitution("fileName");
	}

	public boolean isEnabled()
	{
		final String enabled = this.getSubstitution("enabled");
		return enabled == null || "true".equals(enabled);
	}

	public String getSubstitution(String key)
	{
		return this.substitutions.getProperty(key);
	}

	public void read()
	{
		this.substitutions = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(this.sourceFile.toPath()))
		{
			this.substitutions.load(reader);
		}
		catch (IOException e)
		{
			// TODO better error handling
			e.printStackTrace();
		}
	}

	public void processLines(List<String> lines, PrintStream writer)
	{
		for (String line : lines)
		{
			writer.println(this.processLine(line));
		}
	}

	private String processLine(String line)
	{
		final int length = line.length();
		if (length == 0)
		{
			return line;
		}

		final StringBuilder builder = new StringBuilder(length);

		int current;
		int prev = 0;
		while ((current = line.indexOf('$', prev)) >= 0 && current < length)
		{
			builder.append(line, prev, current); // append contents before $

			int nextIndex = findEndIndex(line, current + 1, length);
			if (nextIndex >= 0)
			{
				final String key = line.substring(current + 1, nextIndex);
				final String replacement = this.getSubstitution(key);
				if (replacement != null)
				{
					builder.append(replacement);
				}
				else
				{
					builder.append(line, current, nextIndex + 1); // append $key$
				}
				prev = nextIndex + 1;
			}
			else
			{
				prev = current + 1;
				builder.append('$');
			}
		}

		// append remaining characters on line
		builder.append(line, prev, length);
		return builder.toString();
	}

	private static int findEndIndex(String line, int startIndex, int length)
	{
		for (int i = startIndex; i < length; i++)
		{
			final char c = line.charAt(i);
			switch (c)
			{
			case '$':
				return i;
			case '.':
			case '_':
			case '-':
				continue;
			}
			if (!Character.isJavaIdentifierPart(c))
			{
				return -1;
			}
		}
		return -1;
	}
}
