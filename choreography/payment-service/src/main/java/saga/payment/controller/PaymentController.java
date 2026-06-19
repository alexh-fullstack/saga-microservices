package saga.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import saga.payment.domain.BalanceEntity;
import saga.payment.domain.BalanceRepository;
import saga.payment.domain.PaymentEntity;
import saga.payment.domain.PaymentRepository;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/balances")
    public List<BalanceEntity> getAllBalances() {
        return balanceRepository.findAll();
    }

    @GetMapping
    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    @PostMapping("/reset")
    public String resetPayments() {
        paymentRepository.deleteAll();
        balanceRepository.deleteAll();
        // Seed default balances
        balanceRepository.save(new BalanceEntity("Alex", 150.00));
        balanceRepository.save(new BalanceEntity("PoorBob", 15.00));
        return "Payments and balances reset successfully";
    }
}
