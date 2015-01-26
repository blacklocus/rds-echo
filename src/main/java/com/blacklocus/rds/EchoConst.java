package com.blacklocus.rds;

public class EchoConst {

    /**
     * Tag that marks an instance as Echo managed.
     */
    public static final String TAG_ECHO_MANAGED_FMT = "rdsecho:%s:managed";

    /**
     * Tag that marks the current Echo stage of an instance.
     */
    public static final String TAG_ECHO_STAGE_FMT = "rdsecho:%s:stage";

    /**
     * The stage that marks an instance as having just been created by means of the RDS restore-from-snapshot API.
     * The next step is to modify it once it becomes available with the instance settings that could not be specified
     * on create.
     */
    public static final String STAGE_NEW = "new";

    /**
     * The stage that marks an instance as having just been modified, but not yet rebooted. Some parameters in the
     * "modify" stage require a reboot to take effect.
     */
    public static final String STAGE_MODIFIED = "modified";

    /**
     * The stage that marks an instance having just been rebooted. All database parameters should have their full
     * effects.
     */
    public static final String STAGE_REBOOTED = "rebooted";

    /**
     * The stage that marks an instance as having been rebooted and ready to be used with all necessary settings. The
     * instance is not yet the target of the CNAME that all participants use to target the particular environment.
     */
    public static final String STAGE_PROMOTED = "promoted";

    /**
     * The stage that marks an instance as finished, and should no longer be used. The instance may be in the process of
     * being destroyed or will be soon.
     */
    public static final String STAGE_RETIRED = "retired";
}
