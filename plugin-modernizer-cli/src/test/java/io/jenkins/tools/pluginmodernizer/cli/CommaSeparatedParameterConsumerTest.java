package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Stack;

import org.junit.jupiter.api.Test;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

public class CommaSeparatedParameterConsumerTest {

    @Test
    public void testSingleValue() {
        CommaSeparatedParameterConsumer consumer = new CommaSeparatedParameterConsumer();
        Stack<String> args = new Stack<>();
        args.push("value1");

        ArgSpec argSpec = PositionalParamSpec.builder().build();
        CommandSpec commandSpec = CommandSpec.create();

        consumer.consumeParameters(args, argSpec, commandSpec);

        List<String> expected = List.of("value1");
        assertEquals(expected, argSpec.getValue());
    }

    @Test
    public void testMultipleValues() {
        CommaSeparatedParameterConsumer consumer = new CommaSeparatedParameterConsumer();
        Stack<String> args = new Stack<>();
        args.push("value1,value2,value3");

        ArgSpec argSpec = PositionalParamSpec.builder().build();
        CommandSpec commandSpec = CommandSpec.create();

        consumer.consumeParameters(args, argSpec, commandSpec);

        List<String> expected = List.of("value1", "value2", "value3");
        assertEquals(expected, argSpec.getValue());
    }

    @Test
    public void testValuesWithSpaces() {
        CommaSeparatedParameterConsumer consumer = new CommaSeparatedParameterConsumer();
        Stack<String> args = new Stack<>();
        args.push("value1, value2 , value3");

        ArgSpec argSpec = PositionalParamSpec.builder().build();
        CommandSpec commandSpec = CommandSpec.create();

        consumer.consumeParameters(args, argSpec, commandSpec);

        List<String> expected = List.of("value1", "value2", "value3");
        assertEquals(expected, argSpec.getValue());
    }

    @Test
    public void testEmptyValue() {
        CommaSeparatedParameterConsumer consumer = new CommaSeparatedParameterConsumer();
        Stack<String> args = new Stack<>();
        args.push("");

        ArgSpec argSpec = PositionalParamSpec.builder().build();
        CommandSpec commandSpec = CommandSpec.create();

        consumer.consumeParameters(args, argSpec, commandSpec);

        List<String> expected = List.of("");
        assertEquals(expected, argSpec.getValue());
    }

    @Test
    public void testTrailingComma() {
        CommaSeparatedParameterConsumer consumer = new CommaSeparatedParameterConsumer();
        Stack<String> args = new Stack<>();
        args.push("value1,");

        ArgSpec argSpec = PositionalParamSpec.builder().build();
        CommandSpec commandSpec = CommandSpec.create();

        consumer.consumeParameters(args, argSpec, commandSpec);

        List<String> expected = List.of("value1", "");
        assertEquals(expected, argSpec.getValue());
    }
}
