package com.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void mainClassIsPresent() {
        assertEquals("com.ratelimiter.Main", Main.class.getName());
    }
}
