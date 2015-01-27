package com.blacklocus.rds;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Callable;

public class Echo {

    private static final Logger LOG = LoggerFactory.getLogger(Echo.class);

    static final Map<String, CommandBundle> COMMANDS = ImmutableMap.<String, CommandBundle>builder()
            .put("new", bundle(EchoNew.class,
                    "Creates a stage '%s' instance from a snapshot. This is usually the longest operation.",
                    EchoConst.STAGE_NEW))
            .put("modify", bundle(EchoModify.class,
                    "Modifies a stage '%s' instance with remaining settings that could not be applied on create and advances stage to '%s'.",
                    EchoConst.STAGE_NEW, EchoConst.STAGE_MODIFIED))
            .put("reboot", bundle(EchoReboot.class,
                    "Reboots a stage '%s' instance so that all settings may take full effect and advances stage to '%s'.",
                    EchoConst.STAGE_MODIFIED, EchoConst.STAGE_REBOOTED))
            .put("promote", bundle(EchoPromote.class,
                    "Promotes a stage '%s' instance so that it becomes the active instance behind the specified CNAME " +
                            "and advances stage to '%s'. Any previously '%s' instances will be moved to stage '%s'.",
                    EchoConst.STAGE_REBOOTED, EchoConst.STAGE_PROMOTED, EchoConst.STAGE_PROMOTED, EchoConst.STAGE_FORGOTTEN))
            .put("retire", bundle(EchoRetire.class,
                    "Retires a stage '%s' instance (destroys it) and advances stage to '%s'.",
                    EchoConst.STAGE_FORGOTTEN, EchoConst.STAGE_RETIRED))
            .build();

    public static void main(String[] args) throws Exception {
        if (args.length < 0) {
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
        PrintWriter p = new PrintWriter(s);

        p.format("usage: rds-echo <command>%n");
        p.format("%n");
        p.format("Valid commands correspond to Echo stages:%n");
        for (Map.Entry<String, CommandBundle> e : COMMANDS.entrySet()) {
            p.format("  %-8s%-70s%n", e.getKey(), e.getValue().description);
        }
        p.format("%n");
        p.format("See the README for more details at https://github.com/blacklocus/rds-echo%n");

        LOG.info(s.toString());
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
