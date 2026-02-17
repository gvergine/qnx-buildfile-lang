package qnx.buildfile.lang.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.utils.Walker;
import qnx.buildfile.lang.utils.Walker.IWalker;

public class DuplicatePathValidator extends BaseDSLValidator
{
	private final static Walker walker = new Walker();

	
	@Override
	public void register(EValidatorRegistrar registrar) {
		// Prevent duplicate registration — this validator is invoked
		// via @ComposedChecks on BuildfileDSLValidator, not directly.
	}

    @Check
	public void checkDuplicates(Model model) {
		Map<String,List<DeploymentStatement>> duplicates = new HashMap<>();

		walker.walk(model, new IWalker() {
			@Override
			public void found(DeploymentStatement deploymentStatement)
			{
				String path = deploymentStatement.getPath();

				if (duplicates.containsKey(path))
				{
					duplicates.get(path).add(deploymentStatement);
				}
				else
				{
					List<DeploymentStatement> l = new ArrayList<>();
					l.add(deploymentStatement);
					duplicates.put(path, l);
				}

			};
		});

		duplicates.forEach((path, deployments) ->{
			if (deployments.size() > 1)
			{
				for (DeploymentStatement deployment : deployments)
				{
					warning("Duplicate path " + path, deployment,  BuildfileDSLPackage.Literals.DEPLOYMENT_STATEMENT__PATH, "duplicatePath");
				}
			}

		});

	}
}
