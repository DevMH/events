package com.devmh.messaging_starter.core;

import java.security.Principal;

public interface FanoutAuthorization {
    boolean canReceive(Principal principal, Envelope event);
}
