package com.mcm.privatecircle.global.exception;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class ConstraintNameResolver {

    private ConstraintNameResolver() {
    }

    public static boolean contains(Throwable throwable, String constraintName) {
        if (throwable == null || constraintName == null || constraintName.isBlank()) {
            return false;
        }

        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current.getMessage() != null && current.getMessage().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
