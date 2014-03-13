package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Zip

class Nar extends Zip {

    public static final String NAR_EXTENSION = "nar";
    Configuration conf

    Nar() {
        setExtension(NAR_EXTENSION);
    }

    void conf(Configuration conf) {
        this.conf = conf
    }
}
