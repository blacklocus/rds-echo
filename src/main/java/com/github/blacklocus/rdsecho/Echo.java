/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 BlackLocus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.blacklocus.rdsecho;

import com.github.blacklocus.rdsecho.instance.EchoModify;
import com.github.blacklocus.rdsecho.instance.EchoNew;
import com.github.blacklocus.rdsecho.instance.EchoPromote;
import com.github.blacklocus.rdsecho.instance.EchoReboot;
import com.github.blacklocus.rdsecho.instance.EchoRetire;
import com.github.blacklocus.rdsecho.instance.EchoSampleOpts;
import com.github.blacklocus.rdsecho.instance.EchoSampleProps;
import com.google.common.collect.ImmutableMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_MODIFY;
import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_NEW;
import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_PROMOTE;
import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_REBOOT;
import static com.github.blacklocus.rdsecho.EchoConst.COMMAND_RETIRE;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_FORGOTTEN;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_MODIFIED;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_NEW;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_PROMOTED;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_REBOOTED;
import static com.github.blacklocus.rdsecho.EchoConst.STAGE_RETIRED;

public class Echo {
    // TODO this only goes to the instance package

    private static final Logger LOG = LoggerFactory.getLogger(Echo.class);

    static final Map<String, CommandBundle> COMMANDS = ImmutableMap.<String, CommandBundle>builder()
            .put("sample-props", bundle(EchoSampleProps.class,
                    "Drops a template rdsecho.properties into the current working directory, which must be " +
                            "fully configured before any other RDS Echo command will function."))
            .put("sample-opts", bundle(EchoSampleOpts.class,
                    "Prints a template RDS_ECHO_OPTS variable to stdout which must be fully configured and then " +
                            "exported before any other RDS Echo command will function. If a rdsecho.properties is " +
                            "present in the current directory, OPTS property values will be populated with the file's " +
                            "values. The stdout of this command can be piped to a file. Log messages are placed on " +
                            "stderr and so will not be included in the output."))
            .put(COMMAND_NEW, bundle(EchoNew.class,
                    "Creates a stage '%s' instance from a snapshot. This is usually the longest operation.",
                    STAGE_NEW))
            .put(COMMAND_MODIFY, bundle(EchoModify.class,
                    "Modifies a stage '%s' instance with remaining settings that could not be applied on create and advances stage to '%s'.",
                    STAGE_NEW, STAGE_MODIFIED))
            .put(COMMAND_REBOOT, bundle(EchoReboot.class,
                    "Reboots a stage '%s' instance so that all settings may take full effect and advances stage to '%s'.",
                    STAGE_MODIFIED, STAGE_REBOOTED))
            .put(COMMAND_PROMOTE, bundle(EchoPromote.class,
                    "Promotes a stage '%s' instance so that it becomes the active instance behind the specified CNAME " +
                            "and advances stage to '%s'. Any previously '%s' instances will be moved to stage '%s'.",
                    STAGE_REBOOTED, STAGE_PROMOTED, STAGE_PROMOTED, STAGE_FORGOTTEN))
            .put(COMMAND_RETIRE, bundle(EchoRetire.class,
                    "Retires a stage '%s' instance (destroys it) and advances stage to '%s'.",
                    STAGE_FORGOTTEN, STAGE_RETIRED))
            .build();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();

        } else if (args.length == 1) {
            String command = args[0];
            CommandBundle bundle = COMMANDS.get(command);

            if (bundle == null) {
                LOG.error("Unrecognized command '{}'.");
                printUsage();

            } else {
                bundle.commandClass.newInstance().call();
            }

        } else {
            LOG.error("Expected exactly one argument.");
            printUsage();
        }
    }

    static void printUsage() {
        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s)
                .format("usage:%n")
                .format("$ rds-echo <command>%n")
                .format("%n")
                .format("RDS Echo may be configured by rdsecho.properties in the current working directory, %n")
                .format("or exporting a fully-populated RDS_ECHO_OPTS environment variable.%n")
                .format("Run 'rds-echo sample-props' or 'rds-echo sample-opts' to get a configuration template.%n")
                .format("%n")
                .format("It is recommended to start with sample-props as that template includes documentation about%n")
                .format("many of the parameters. This can then be converted to OPTS if desired through the%n")
                .format("sample-opts command.%n")
                .format("%n")
                .format("Valid commands correspond to Echo stages:%n")
                .format("%n");

        for (Map.Entry<String, CommandBundle> e : COMMANDS.entrySet()) {
            List<String> descriptionLines = wrap(e.getValue().description, 84); // for a total of 100
            p.format("  %-14s%s%n", e.getKey(), descriptionLines.get(0));
            for (int i = 1; i < descriptionLines.size(); i++) {
                p.format("  %-14s%s%n", "", descriptionLines.get(i));
            }
            p.format("%n");
        }

        p.format("%n");
        p.format("See the README for more details at https://github.com/blacklocus/rds-echo%n");

        LOG.info(s.toString());
    }

    static List<String> wrap(String description, int width) {
        // rough-hewn code
        Matcher m = Pattern.compile("\\S+\\s*").matcher(description);
        StringBuilder line = new StringBuilder(width);
        List<String> lines = new ArrayList<String>();
        while (m.find()) {
            String group = m.group();
            line = wrapLineHelper(line, lines, group, width);
            line.append(group);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    static StringBuilder wrapLineHelper(StringBuilder line, List<String> lines, String group, int width) {
        if (line.length() + group.length() > width) {
            lines.add(line.toString());
            line = new StringBuilder(width);
        }
        return line;
    }

    static CommandBundle bundle(Class<? extends Callable<Boolean>> commandClass, String descriptionFormat, Object... formatArgs) {
        return new CommandBundle(commandClass, String.format(descriptionFormat, formatArgs));
    }

    static class CommandBundle {
        final Class<? extends Callable<Boolean>> commandClass;
        final String description;

        CommandBundle(Class<? extends Callable<Boolean>> commandClass, String description) {
            this.commandClass = commandClass;
            this.description = description;
        }
    }
}
