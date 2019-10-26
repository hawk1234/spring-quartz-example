package com.mz.example;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class TestUtil {

    public static void conditionalWait(Supplier<Boolean> waitIfTrue) throws InterruptedException{
        int timeout = 30*1000;
        int ping = 100;
        int waitTime = 0;
        while (waitIfTrue.get()){
            //Not the cleanest solution, but will serve for test purposes
            Thread.sleep(ping);
            waitTime += ping;
            if(waitTime > timeout){
                throw new IllegalStateException("Waiting on condition exceeded estimated wait time.");
            }
        }
    }
}
