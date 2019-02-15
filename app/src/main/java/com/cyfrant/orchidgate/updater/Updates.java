package com.cyfrant.orchidgate.updater;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Updates {
    private String certificate;
    private List<Update> updates;

    public Updates() {
        this.updates = new CopyOnWriteArrayList<>();
    }
}
