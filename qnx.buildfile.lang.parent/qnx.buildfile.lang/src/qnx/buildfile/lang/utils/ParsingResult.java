package qnx.buildfile.lang.utils;

import java.util.List;

import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.Issue;

import qnx.buildfile.lang.buildfileDSL.Model;

public class ParsingResult
{
	public final List<Issue> issues;
	public final Model model;

	public ParsingResult(List<Issue> issues, Model model)
	{
		this.issues = issues;
		this.model = model;
	}

	public boolean hasErrors()
	{
		return issues.stream().map(Issue::getSeverity).filter(Severity.ERROR::equals).count() != 0;
	}

	public boolean noErrors()
	{
		return !hasErrors();
	}
}