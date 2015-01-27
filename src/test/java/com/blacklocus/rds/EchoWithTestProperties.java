package com.blacklocus.rds;

public class EchoWithTestProperties {

    public static class New {
        public static void main(String[] args) throws Exception {
            EchoNew.main(args);
        }
    }

    public static class Modify {
        public static void main(String[] args) throws Exception {
            EchoModify.main(args);
        }
    }

    public static class Reboot {
        public static void main(String[] args) throws Exception {
            EchoReboot.main(args);
        }
    }

    public static class Promote {
        public static void main(String[] args) throws Exception {
            EchoPromote.main(args);
        }
    }

    public static class Retire {
        public static void main(String[] args) throws Exception {
            EchoRetire.main(args);
        }
    }

}
