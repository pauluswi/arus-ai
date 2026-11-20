package com.pswied.core.reconciliation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation/batches")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReconciliationBatchResponse run(Principal principal) {
        return reconciliationService.run(principal == null ? "anonymous" : principal.getName());
    }

    @GetMapping
    public List<ReconciliationBatchResponse> list() {
        return reconciliationService.list();
    }

    @GetMapping("/{batchId}")
    public ReconciliationBatchResponse get(@PathVariable UUID batchId) {
        return reconciliationService.get(batchId);
    }

    @GetMapping("/{batchId}/results")
    public List<ReconciliationResultResponse> results(@PathVariable UUID batchId) {
        return reconciliationService.results(batchId);
    }
}
