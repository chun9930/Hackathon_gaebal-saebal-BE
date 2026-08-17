package com.mcm.privatecircle.ai.client;

import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.dto.GeminiBriefResult;

public interface GeminiBriefClient {

    GeminiBriefResult generate(AiBriefSource source);
}
