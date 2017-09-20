package com.sharedpreferencesmanagerdemo;

import com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager;

@SPManager
class SPModule {

    private static SPModule mInstnace;

    private SPModule() {
    }

    public static SPModule getInstance() {
        return null;
    }
}
