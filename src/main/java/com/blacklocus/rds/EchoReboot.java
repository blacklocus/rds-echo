package com.blacklocus.rds;

import java.util.concurrent.Callable;

public class EchoReboot implements Callable<Boolean> {

    @Override
    public Boolean call() throws Exception {
        return null; //TODO jason
    }

    public static void main(String[] args) throws Exception {
        new EchoReboot().call();
    }
}
