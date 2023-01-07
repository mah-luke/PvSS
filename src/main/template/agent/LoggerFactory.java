package old.agent;

import at.ac.tuwien.ifs.sge.engine.Logger;

public class LoggerFactory {

    public static Logger getLogger(int logLevel, String pre) {
        return new Logger(
                logLevel,
                pre,
                "",
                "trace]: ",
                System.out,
                "",
                "debug]: ",
                System.out,
                "",
                "info]: ",
                System.out,
                "",
                "warn]: ",
                System.err,
                "",
                "error]: ",
                System.err,
                ""
        );
    }
}
