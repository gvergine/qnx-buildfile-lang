package qnx.buidfile.lang.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qnx.buidfile.lang.utils.Walker.IWalker;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.buildfileDSL.Path;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

public class VariableSubstitutor
{
	private final static Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");

    private static String substituteEnvVars(String input, Map<String,String> varMap) {
        if (input == null) return null;

        Matcher matcher = pattern.matcher(input);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = varMap.getOrDefault(varName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }

        matcher.appendTail(result);
        return result.toString();
    }
    
    public void substituteVariables(Model mode, Map<String,String> varMap)
    {
    	new Walker().walk(mode, new IWalker() {
    		
    		@Override
    		public void found(ValuedAttribute valuedAttribute)
    		{
    			valuedAttribute.setValue(substituteEnvVars(valuedAttribute.getValue(), varMap));
    		}
    		
    		@Override
    		public void found(DeploymentStatement deploymentStatement)
    		{
    			deploymentStatement.setPath(substituteEnvVars(deploymentStatement.getPath(), varMap));
    		}
    		
    		@Override
    		public void found(Path path)
    		{
    			path.setValue(substituteEnvVars(path.getValue(), varMap));
    		}
    		
    	});
    }

}
