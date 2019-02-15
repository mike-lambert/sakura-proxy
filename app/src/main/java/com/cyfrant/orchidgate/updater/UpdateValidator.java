package com.cyfrant.orchidgate.updater;

import android.app.Application;

import java.io.File;

public class UpdateValidator {
    private final Update reference;
    private final File apk;
    private final Application application;

    public UpdateValidator(Application self, Update update, File installationPackage) {
        apk = installationPackage;
        reference = update;
        application = self;
    }

    public String validateUpdate() {

    }
}
