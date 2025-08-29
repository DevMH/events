package com.devmh.messaging_starter.core;

import java.security.Principal;
import java.util.Map;

public interface PrincipalResolver {
    Principal resolve(Principal original, Map<String,Object> wsHandshakeAttributes);
}
