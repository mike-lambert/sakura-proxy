package com.cyfrant.orchidgate.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Background {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static ExecutorService threadPool() {
        return threadPool;
    }

    private Background() {
    }
}
