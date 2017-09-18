package com.sharedpreferencesmanagerdemo;

import com.sharedpreferencesmanager.annotations.SPManager;

@SPManager
class SPModule {

    private static SPModule mInstnace;

    private SPModule() {
    }

    public static SPModule getInstance() {
        return null;
    }
}
