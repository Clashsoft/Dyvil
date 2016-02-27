package dyvil.tools.repl.command;

import dyvil.reflect.ReflectUtils;
import dyvil.tools.repl.DyvilREPL;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavapCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "javap";
	}

	@Override
	public String getDescription()
	{
		return "Shows the Javap Decompiler Output for the given class";
	}

	@Override
	public String getUsage()
	{
		return ":javap <classname>";
	}

	@Override
	public void execute(DyvilREPL repl, String argument)
	{
		if (argument == null)
		{
			repl.getErrorOutput().println("Missing Class Argument. Usage: " + this.getUsage());
			return;
		}

		// Construct the Command
		String[] command = getCommand(repl, argument.split(" "));
		if (command == null)
		{
			return;
		}

		// Run the Command
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);

		try
		{
			Process process = processBuilder.start();

			// Read the Process Output
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					repl.getOutput().println(line);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace(repl.getErrorOutput());
		}
	}

	public static String[] getCommand(DyvilREPL repl, String[] args)
	{
		String className = args[0];
		Class theClass;
		File location;

		try
		{
			theClass = Class.forName(className, false, ClassLoader.getSystemClassLoader());
			location = ReflectUtils.getFileLocation(theClass);
		}
		catch (ClassNotFoundException ex)
		{
			repl.getOutput().println("Could not find Class '" + className + "'");
			return null;
		}

		final int length = args.length;

		String[] command = new String[4 + length - 1];
		command[0] = "javap";
		System.arraycopy(args, 1, command, 1, length - 1);
		command[length] = "-cp";
		command[length + 1] = location.toString();
		command[length + 2] = theClass.getName();
		return command;
	}
}
