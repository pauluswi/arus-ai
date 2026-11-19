package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentNetworkSubmissionController {

    private final PaymentNetworkSubmissionService submissionService;

    public PaymentNetworkSubmissionController(PaymentNetworkSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/payments/{paymentRecordId}/network-submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NetworkSubmissionStatusView submit(@PathVariable UUID paymentRecordId) {
        return submissionService.submit(paymentRecordId);
    }

    @GetMapping("/network-submissions/{submissionId}")
    public NetworkSubmissionStatusView get(@PathVariable UUID submissionId) {
        return submissionService.get(submissionId);
    }
}
