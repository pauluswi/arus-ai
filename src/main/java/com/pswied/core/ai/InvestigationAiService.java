package com.pswied.core.ai;

import java.util.UUID;

public interface InvestigationAiService {

    InvestigationReport generateReport(UUID exceptionId);
}
