package io.jenkins.tools.pluginmodernizer.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

public class CommaSeparatedParameterConsumer implements IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
        String value = args.pop();
        List<String> result = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            result.add(item.trim());
        }
        argSpec.setValue(result);
    }
}
