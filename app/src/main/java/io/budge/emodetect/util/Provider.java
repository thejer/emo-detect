package io.budge.emodetect.util;

import android.content.Context;

import io.budge.emodetect.data.database.LocalDatabase;


public final class Provider {

    private static LocalDatabase localDatabase;
    private Provider() {
    }

    public static LocalDatabase provideLocalDatabase(Context context) {
        if (localDatabase == null) {
            localDatabase = LocalDatabase.getsInstance(context);
        }
        return localDatabase;
    }

}
