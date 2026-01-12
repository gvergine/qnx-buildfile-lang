package qnx.buildfile.lang.cli;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.xtext.validation.Issue;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import qnx.buildfile.lang.utils.Parser;
import qnx.buildfile.lang.utils.ParsingResult;
import qnx.buildfile.lang.validation.BuildfileDSLValidator;

public class Main implements Callable<Integer>
{
	@Option(
			names = "-i",
			description = "buildfile",
			required = true,
			split = ","
			)
	private List<String> inputs = new ArrayList<>();

	@Option(
			names = "-c",
			description = "custom validator jar",
			required = false
			)
	private File customValidator;



	@Override
	public Integer call() throws Exception
	{
		Class<? extends BuildfileDSLValidator> validatorClass = BuildfileDSLValidator.class;

		if (customValidator != null)
		{
			System.out.println("Using custom validator " + customValidator);
			validatorClass = loadValidatorFromJar(customValidator);
		}

		Integer failures = 0;
		Parser parser = new Parser(validatorClass);

		for (String filename : inputs)
		{
			System.out.println("Processing " + filename);
			File file = new File(filename);

			if(!file.exists())
			{
				throw new FileNotFoundException(filename);
			}

			ParsingResult parseResult = parser.parse(file);        	
			parseResult.issues.forEach(issue -> printIssue(filename, issue));

			if (parseResult.hasErrors())
			{
				failures++;
			}

			System.out.println("Done - " + failures + " failure" + ((failures == 1) ? "" : "s"));
		}

		return failures;  
	}

	private void printIssue(String filename, Issue issue)
	{
		System.err.println(issue.getSeverity() + " at " + filename + ":" + issue.getLineNumber() + ": " + issue.getMessage());
	}

	private static URLClassLoader classLoader;

	private static Class<? extends BuildfileDSLValidator> loadValidatorFromJar(File jarFile) throws Exception
	{
		if (!jarFile.exists())
		{
			throw new IllegalArgumentException("Jar file not found: " + jarFile);
		}

		String mainClassName;
		try (JarFile jar = new JarFile(jarFile))
		{
			Manifest manifest = jar.getManifest();
			if (manifest == null)
			{
				throw new IllegalStateException("No MANIFEST.MF found");
			}

			mainClassName = manifest.getMainAttributes().getValue("Main-Class");

			if (mainClassName == null)
			{
				throw new IllegalStateException("Main-Class not defined in MANIFEST.MF");
			}
		}

		URL jarUrl = jarFile.toURI().toURL();
		classLoader = new URLClassLoader(new URL[]{jarUrl}, ClassLoader.getSystemClassLoader());

		Class<?> mainClass = Class.forName(mainClassName, true, classLoader);

		if (!BuildfileDSLValidator.class.isAssignableFrom(mainClass))
		{
			throw new IllegalStateException(
					"Main-Class " + mainClassName + " does not extend BuildfileDSLValidator"
					);
		}

		return (Class<? extends BuildfileDSLValidator>) mainClass;


	}

	public static void main(String[] args)
	{
		String version = Main.class.getPackage().getImplementationVersion();
		System.out.println("QNX Buildfile Validator version " + version);
		int exitCode = new CommandLine(new Main()).execute(args);        
		System.exit(exitCode == 0 ? 0 : 1);
	}
}
