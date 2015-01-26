package com.blacklocus.rds;

import com.amazonaws.services.rds.model.DBInstance;

public class EchoRetire extends AbstractEchoIntermediateStage {

    public EchoRetire() {
        super(EchoConst.STAGE_PROMOTED, EchoConst.STAGE_RETIRED);
    }

    @Override
    boolean traverseStage(DBInstance instance) {
        return false; //TODO jason
    }

    public static void main(String[] args) throws Exception {
        new EchoRetire().call();
    }
}
