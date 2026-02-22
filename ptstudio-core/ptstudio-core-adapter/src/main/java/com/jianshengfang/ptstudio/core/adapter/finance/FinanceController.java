package com.jianshengfang.ptstudio.core.adapter.finance;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.finance.FinanceService;
import com.jianshengfang.ptstudio.core.app.finance.InMemoryFinanceStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/api/orders")
    @AuditAction(module = "ORDER", action = "CREATE")
    public InMemoryFinanceStore.OrderData createOrder(@Valid @RequestBody CreateOrderRequest request) {
        TenantStoreContext context = requireContext();
        return financeService.createOrder(new FinanceService.CreateOrderCommand(
                context.tenantId(),
                context.storeId(),
                request.memberId(),
                request.orderType(),
                request.totalAmount()
        ));
    }

    @GetMapping("/api/orders")
    public List<InMemoryFinanceStore.OrderData> listOrders() {
        TenantStoreContext context = requireContext();
        return financeService.listOrders(context.tenantId(), context.storeId());
    }

    @GetMapping("/api/orders/{id}")
    public InMemoryFinanceStore.OrderData orderDetail(@PathVariable Long id) {
        TenantStoreContext context = requireContext();
        return financeService.getOrder(id, context.tenantId(), context.storeId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    @PostMapping("/api/payments/alipay/precreate")
    @AuditAction(module = "PAYMENT", action = "ALIPAY_PRECREATE")
    public PrecreateResponse precreate(@Valid @RequestBody PrecreateRequest request) {
        TenantStoreContext context = requireContext();
        InMemoryFinanceStore.PaymentData payment = financeService.precreateAlipay(
                request.orderId(), context.tenantId(), context.storeId());
        return new PrecreateResponse(payment.id(), payment.orderId(), payment.amount(), payment.payStatus(),
                "alipay://qrcode/" + payment.id());
    }

    @PostMapping("/api/payments/alipay/callback")
    @AuditAction(module = "PAYMENT", action = "ALIPAY_CALLBACK")
    public InMemoryFinanceStore.PaymentData alipayCallback(@Valid @RequestBody CallbackRequest request) {
        TenantStoreContext context = requireContext();
        return financeService.alipayCallback(new FinanceService.PaymentCallbackCommand(
                context.tenantId(),
                context.storeId(),
                request.orderId(),
                request.channelTradeNo(),
                request.callbackRaw()
        ));
    }

    @PostMapping("/api/refunds")
    @AuditAction(module = "REFUND", action = "CREATE")
    public InMemoryFinanceStore.RefundData createRefund(@Valid @RequestBody CreateRefundRequest request) {
        TenantStoreContext context = requireContext();
        return financeService.createRefund(new FinanceService.CreateRefundCommand(
                context.tenantId(),
                context.storeId(),
                request.orderId(),
                request.refundAmount(),
                request.reason()
        ));
    }

    @PostMapping("/api/refunds/{id}/approve")
    @AuditAction(module = "REFUND", action = "APPROVE")
    public InMemoryFinanceStore.RefundData approve(@PathVariable Long id,
                                                   @Valid @RequestBody RefundDecisionRequest request) {
        TenantStoreContext context = requireContext();
        return financeService.approveRefund(id, context.tenantId(), context.storeId(), request.operatorUserId());
    }

    @PostMapping("/api/refunds/{id}/reject")
    @AuditAction(module = "REFUND", action = "REJECT")
    public InMemoryFinanceStore.RefundData reject(@PathVariable Long id,
                                                  @Valid @RequestBody RefundDecisionRequest request) {
        TenantStoreContext context = requireContext();
        return financeService.rejectRefund(id, context.tenantId(), context.storeId(), request.operatorUserId());
    }

    @GetMapping("/api/reconciliations/daily")
    public InMemoryFinanceStore.DailyReconcileData daily(@RequestParam(name = "bizDate", required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                         LocalDate bizDate) {
        TenantStoreContext context = requireContext();
        return financeService.dailyReconcile(context.tenantId(), context.storeId(), bizDate);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreateOrderRequest(@NotNull Long memberId,
                                     @NotBlank String orderType,
                                     @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount) {
    }

    public record PrecreateRequest(@NotNull Long orderId) {
    }

    public record PrecreateResponse(Long paymentId,
                                    Long orderId,
                                    BigDecimal amount,
                                    String payStatus,
                                    String qrCode) {
    }

    public record CallbackRequest(@NotNull Long orderId,
                                  @NotBlank String channelTradeNo,
                                  @NotBlank String callbackRaw) {
    }

    public record CreateRefundRequest(@NotNull Long orderId,
                                      @NotNull @DecimalMin(value = "0.01") BigDecimal refundAmount,
                                      @NotBlank String reason) {
    }

    public record RefundDecisionRequest(@NotNull Long operatorUserId) {
    }
}
