package com.extantape.paulbunyan.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PrismLogTest {
    public interface Builder {
        Builder cause(String who);

        String build();
    }

    /** Like Prism's builder: public methods on a class that isn't public. Found on the 26.2 clone. */
    private static final class HiddenBuilder implements Builder {
        String who = "";

        @Override
        public Builder cause(String w) {
            who = w;
            return this;
        }

        @Override
        public String build() {
            return "built for " + who;
        }
    }

    @Test
    void callsPublicMethodsOfAHiddenClassThroughItsInterface() throws Exception {
        Object b = new HiddenBuilder();
        b = PrismLog.call(b, "cause", "Steve");
        assertEquals("built for Steve", PrismLog.call(b, "build"));
    }

    @Test
    void publicTypesSkipTheHiddenClass() {
        var types = PrismLog.publicTypes(HiddenBuilder.class);
        assertFalse(types.contains(HiddenBuilder.class));
        assertTrue(types.contains(Builder.class));
    }
}
