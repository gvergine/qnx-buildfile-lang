package qnx.buildfile.lang.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.xtext.validation.Issue;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import qnx.buildfile.lang.utils.Parser;
import qnx.buildfile.lang.utils.ParsingResult;
import qnx.buildfile.lang.validation.SystemPropertyCustomValidatorJarPathProvider;

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
		if (customValidator != null)
		{
			if (!customValidator.exists())
			{
				throw new FileNotFoundException("Custom validator JAR not found: " + customValidator);
			}
			System.out.println("Using custom validator " + customValidator);
			System.setProperty(SystemPropertyCustomValidatorJarPathProvider.SYSTEM_PROPERTY,
					customValidator.getAbsolutePath());
		}

		Parser parser = new Parser();
		Integer failures = 0;

		for (String filename : inputs)
		{
			System.out.println("Processing " + filename);
			File file = new File(filename);

			if (!file.exists())
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

	public static void main(String[] args)
	{
		String version = Main.class.getPackage().getImplementationVersion();
		System.out.println("QNX Buildfile Validator version " + version);
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode == 0 ? 0 : 1);
	}
}
