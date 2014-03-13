package me.sgeb.gradle.nativeartifacts

class TestHelpers {

    static <T> T one(Collection<T> collection) {
        assert collection.size() == 1
        return collection.iterator().next()
    }
}
