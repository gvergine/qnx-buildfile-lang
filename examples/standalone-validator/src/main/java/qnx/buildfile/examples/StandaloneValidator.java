package qnx.buildfile.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Injector;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import qnx.buildfile.lang.BuildfileDSLStandaloneSetup;
import qnx.buildfile.lang.buildfileDSL.AttributeSection;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;
import qnx.buildfile.lang.utils.VariableSubstitutor;
import qnx.buildfile.lang.utils.Walker;
import qnx.buildfile.lang.validation.SystemPropertyCustomValidatorJarPathProvider;

/**
 * Standalone buildfile validator demonstrating how to use the
 * qnx-buildfile-lang library for:
 * <ul>
 *   <li>Parsing buildfiles into a model</li>
 *   <li>Substituting {@code ${VAR}} references from environment variables</li>
 *   <li>Running standard and/or custom validation</li>
 *   <li>Detecting directory deployments and suggesting file-level replacements</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   java -jar standalone-validator.jar [OPTIONS] file1.build [file2.build ...]
 * </pre>
 */
@Command(
		name = "standalone-validator",
		description = "Validates QNX buildfiles with variable substitution and directory analysis.",
		mixinStandardHelpOptions = true,
		version = "standalone-validator 0.1.0"
		)
public class StandaloneValidator implements Callable<Integer> {

	private static final Pattern VARREF_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

	@Parameters(
			paramLabel = "FILE",
			description = "One or more buildfiles to validate.",
			arity = "1..*"
			)
	private List<File> inputFiles;

	@Option(
			names = {"-c", "--custom-validator"},
			description = "Path to a custom validator JAR."
			)
	private File customValidatorJar;

	@Option(
			names = {"-W", "--fail-on-warning"},
			description = "Treat warnings as errors (exit 1 if any warnings)."
			)
	private boolean failOnWarning;

	@Option(
			names = {"--strict-vars"},
			description = "Fail if any ${VAR} reference cannot be resolved from the environment."
			)
	private boolean strictVars;

	@Option(
			names = {"-e", "--env"},
			description = "Additional variable in KEY=VALUE format (repeatable).",
			split = ","
			)
	private List<String> extraVars = new ArrayList<>();

	@Option(
			names = {"-r", "--report"},
			description = "Write report to a file instead of stdout."
			)
	private File reportFile;

	@Option(
			names = {"-q", "--quiet"},
			description = "Suppress informational output; only show errors and warnings."
			)
	private boolean quiet;

	/** Xtext injector — created once, used for all files. */
	private Injector injector;
	private ResourceSet resourceSet;
	private IResourceValidator validator;

	/** Collects report lines. */
	private final List<String> reportLines = new ArrayList<>();

	@Override
	public Integer call() throws Exception {
		initXtext();

		int totalErrors = 0;
		int totalWarnings = 0;

		for (File inputFile : inputFiles) {
			if (!inputFile.exists()) {
				throw new FileNotFoundException("File not found: " + inputFile);
			}

			FileResult result = processFile(inputFile);
			totalErrors += result.errors;
			totalWarnings += result.warnings;
		}

		// Write report
		writeReport();

		// Summary
		if (!quiet) {
			System.out.println();
			System.out.println("Summary: " + totalErrors + " error(s), " + totalWarnings + " warning(s)");
		}

		if (totalErrors > 0) return 1;
		if (failOnWarning && totalWarnings > 0) return 1;
		return 0;
	}

	/**
	 * Initialise the Xtext runtime (parser, validator, injector).
	 */
	private void initXtext() throws FileNotFoundException {
		if (customValidatorJar != null) {
			if (!customValidatorJar.exists()) {
				throw new FileNotFoundException(
						"Custom validator JAR not found: " + customValidatorJar);
			}
			System.setProperty(SystemPropertyCustomValidatorJarPathProvider.SYSTEM_PROPERTY,
					customValidatorJar.getAbsolutePath());
			if (!quiet) {
				System.out.println("Using custom validator: " + customValidatorJar);
			}
		}

		injector = new BuildfileDSLStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
		validator = injector.getInstance(IResourceValidator.class);
	}

	/**
	 * Build the variable map: environment variables + any {@code -e KEY=VALUE} overrides.
	 */
	private Map<String, String> buildVarMap() {
		Map<String, String> varMap = new LinkedHashMap<>(System.getenv());
		for (String kv : extraVars) {
			int eq = kv.indexOf('=');
			if (eq > 0) {
				varMap.put(kv.substring(0, eq), kv.substring(eq + 1));
			}
		}
		return varMap;
	}

	/**
	 * Process a single buildfile through the full pipeline.
	 */
	private FileResult processFile(File file) throws IOException {
		if (!quiet) {
			System.out.println("* " + file.getName() + " *");
		}

		int errors = 0;
		int warnings = 0;

		// Step 1: Parse (syntax only — load the model)
		Resource resource = resourceSet.getResource(
				org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath()),
				true);
		resource.load(null);
		Model model = (Model) resource.getContents().get(0);

		if (!resource.getErrors().isEmpty()) {
			for (Resource.Diagnostic d : resource.getErrors()) {
				printDiag(file, "ERROR", d.getLine(), d.getMessage());
				errors++;
			}
		}

		if (errors > 0) return new FileResult(errors, 0);

		// Step 2: Variable substitution
		Map<String, String> varMap = buildVarMap();
		new VariableSubstitutor().substituteVariables(model, varMap);

		// Check for unresolved variables
		List<UnresolvedVar> unresolvedVars = findUnresolvedVariables(model);
		if (!unresolvedVars.isEmpty()) {
			for (UnresolvedVar uv : unresolvedVars) {
				String msg = "Unresolved variable ${" + uv.varName + "} in " + uv.context;
				if (strictVars) {
					printIssue(file, "ERROR", msg);
					errors++;
				} else {
					printIssue(file, "WARNING", msg);
					warnings++;
				}
			}
		}

		// ── Step 3: Validation ────────────────────────────────────
		List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		for (Issue issue : issues) {
			printXtextIssue(file, issue);
			if (issue.getSeverity() == Severity.ERROR) errors++;
			if (issue.getSeverity() == Severity.WARNING) warnings++;
		}

		// ── Step 4: Directory deployment analysis ─────────────────
		List<DirectoryDeployment> dirDeployments = findDirectoryDeployments(model);
		for (DirectoryDeployment dd : dirDeployments) {
			String msg = "Directory deployment: " + dd.targetPath + " = " + dd.sourcePath;
			printIssue(file, "WARNING", msg);
			warnings++;

			List<String> suggestion = suggestFileDeployments(dd);
			if (!suggestion.isEmpty()) {
				reportLines.add("  Suggestion — replace with individual file deployments:");
				for (String line : suggestion) {
					reportLines.add("    " + line);
				}
			} else {
				reportLines.add("  (source directory not found or empty on this host)");
			}
		}

		if (!quiet) {
			System.out.println("  " + errors + " error(s), " + warnings + " warning(s)");
		}

		// Clean up resource for next file
		resource.unload();
		resourceSet.getResources().remove(resource);

		return new FileResult(errors, warnings);
	}

	// ── Unresolved variable detection ─────────────────────────────

	/**
	 * Walk the model and find any remaining ${VAR} references that were
	 * not resolved during substitution.
	 */
	private List<UnresolvedVar> findUnresolvedVariables(Model model) {
		List<UnresolvedVar> unresolved = new ArrayList<>();

		new Walker().walk(model, new Walker.IWalker() {
			@Override
			public void found(DeploymentStatement ds) {
				collectUnresolved(ds.getPath(), "path '" + ds.getPath() + "'", unresolved);
			}

			@Override
			public void found(qnx.buildfile.lang.buildfileDSL.Path path) {
				collectUnresolved(path.getValue(), "content '" + path.getValue() + "'", unresolved);
			}

			@Override
			public void found(ValuedAttribute va) {
				collectUnresolved(va.getValue(),
						"attribute " + va.getName() + "=" + va.getValue(), unresolved);
			}
		});

		return unresolved;
	}

	private void collectUnresolved(String text, String context, List<UnresolvedVar> out) {
		if (text == null) return;
		Matcher m = VARREF_PATTERN.matcher(text);
		while (m.find()) {
			out.add(new UnresolvedVar(m.group(1), context));
		}
	}

	// Directory deployment detection

	/**
	 * Find deployment statements that copy an entire directory from the host.
	 * A directory deployment is identified by {@code type=dir} in the attribute
	 * section AND a source path (content) that is present.
	 * But also all the paths are checked if they are dirs.
	 */
	private List<DirectoryDeployment> findDirectoryDeployments(Model model) {
		List<DirectoryDeployment> result = new ArrayList<>();

		new Walker().walk(model, new Walker.IWalker() {
			@Override
			public void found(DeploymentStatement ds) {
				if (ds.getContent() == null || !(ds.getContent() instanceof qnx.buildfile.lang.buildfileDSL.Path)) {
					return;
				}

				AttributeSection attrs = ds.getAttributesection();

				String sourcePath = ((qnx.buildfile.lang.buildfileDSL.Path) ds.getContent()).getValue();

				if (new File(sourcePath).exists())
				{
					if (new File(sourcePath).isDirectory())
					{
						result.add(new DirectoryDeployment(ds.getPath(), sourcePath, attrs));
					}
				}
				else if (attrs != null) // even if not found, could still be declared as a dir
				{
					boolean isTypeDir = attrs.getAttributes().stream()
							.filter(a -> a instanceof ValuedAttribute)
							.map(a -> (ValuedAttribute) a)
							.anyMatch(va -> "type".equals(va.getName()) && "dir".equals(va.getValue()));

					if (isTypeDir)
						result.add(new DirectoryDeployment(ds.getPath(), sourcePath, attrs));

				}
				//                }
			}
		});

		return result;
	}

	/**
	 * Given a directory deployment, check whether the source path exists on the host
	 * and list its files. Return suggested replacement deployment lines.
	 */
	private List<String> suggestFileDeployments(DirectoryDeployment dd) {
		Path sourceDir = Path.of(dd.sourcePath);
		if (!Files.isDirectory(sourceDir)) {
			return List.of();
		}

		try (Stream<Path> entries = Files.walk(sourceDir)) {
		    return entries
		            .filter(Files::isRegularFile)
		            .sorted()
		            .map(f -> {
		                String relativePath = sourceDir.relativize(f).toString();
		                String targetFile = dd.targetPath.endsWith("/")
		                        ? dd.targetPath + relativePath
		                        : dd.targetPath + "/" + relativePath;
		                return targetFile + " = " + f;
		            })
		            .collect(Collectors.toList());
		} catch (IOException e) {
			return List.of();
		}
	}

	// ── Output helpers ────────────────────────────────────────────

	private void printXtextIssue(File file, Issue issue) {
		String severity = issue.getSeverity().name();
		String location = file.getName() + ":" + issue.getLineNumber();
		String line = severity + " at " + location + ": " + issue.getMessage();
		System.err.println(line);
		reportLines.add(line);
	}

	private void printDiag(File file, String severity, int line, String message) {
		String text = severity + " at " + file.getName() + ":" + line + ": " + message;
		System.err.println(text);
		reportLines.add(text);
	}

	private void printIssue(File file, String severity, String message) {
		String text = severity + " in " + file.getName() + ": " + message;
		System.err.println(text);
		reportLines.add(text);
	}

	private void writeReport() throws IOException {
		if (reportFile != null && !reportLines.isEmpty()) {
			try (PrintStream ps = new PrintStream(reportFile)) {
				for (String line : reportLines) {
					ps.println(line);
				}
			}
			if (!quiet) {
				System.out.println("Report written to: " + reportFile);
			}
		}
	}

	// ── Data holders ──────────────────────────────────────────────

	private record FileResult(int errors, int warnings) {}

	private record UnresolvedVar(String varName, String context) {}

	private record DirectoryDeployment(
			String targetPath,
			String sourcePath,
			AttributeSection attributes
			) {}

	// ── Entry point ───────────────────────────────────────────────

	public static void main(String[] args) {
		int exitCode = new CommandLine(new StandaloneValidator()).execute(args);
		System.exit(exitCode);
	}
}
