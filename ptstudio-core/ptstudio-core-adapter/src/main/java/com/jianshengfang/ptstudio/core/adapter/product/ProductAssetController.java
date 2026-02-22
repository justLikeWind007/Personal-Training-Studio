package com.jianshengfang.ptstudio.core.adapter.product;

import com.jianshengfang.ptstudio.core.adapter.audit.AuditAction;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContext;
import com.jianshengfang.ptstudio.core.app.context.TenantStoreContextHolder;
import com.jianshengfang.ptstudio.core.app.product.ProductAssetModels;
import com.jianshengfang.ptstudio.core.app.product.ProductAssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
public class ProductAssetController {

    private final ProductAssetService productAssetService;

    public ProductAssetController(ProductAssetService productAssetService) {
        this.productAssetService = productAssetService;
    }

    @GetMapping("/api/packages")
    public List<ProductAssetModels.PackageData> listPackages() {
        TenantStoreContext context = requireContext();
        return productAssetService.listPackages(context.tenantId(), context.storeId());
    }

    @PostMapping("/api/packages")
    @AuditAction(module = "PACKAGE", action = "CREATE")
    public ProductAssetModels.PackageData createPackage(@Valid @RequestBody CreatePackageRequest request) {
        TenantStoreContext context = requireContext();
        return productAssetService.createPackage(
                context.tenantId(), context.storeId(),
                request.packageName(), request.totalSessions(), request.validDays(), request.price(), request.saleStatus()
        );
    }

    @PutMapping("/api/packages/{id}")
    @AuditAction(module = "PACKAGE", action = "UPDATE")
    public ProductAssetModels.PackageData updatePackage(@PathVariable Long id,
                                                        @Valid @RequestBody UpdatePackageRequest request) {
        TenantStoreContext context = requireContext();
        return productAssetService.updatePackage(
                id, context.tenantId(), context.storeId(),
                request.packageName(), request.totalSessions(), request.validDays(), request.price(), request.saleStatus()
        );
    }

    @GetMapping("/api/members/{id}/packages")
    public List<ProductAssetModels.MemberPackageData> listMemberPackages(@PathVariable("id") Long memberId) {
        TenantStoreContext context = requireContext();
        return productAssetService.listMemberPackages(context.tenantId(), context.storeId(), memberId);
    }

    @GetMapping("/api/members/{id}/package-ledgers")
    public List<ProductAssetModels.PackageLedgerData> listMemberPackageLedgers(@PathVariable("id") Long memberId) {
        TenantStoreContext context = requireContext();
        return productAssetService.listMemberPackageLedgers(context.tenantId(), context.storeId(), memberId);
    }

    private TenantStoreContext requireContext() {
        TenantStoreContext context = TenantStoreContextHolder.get();
        if (context == null) {
            throw new IllegalArgumentException("缺少租户门店上下文");
        }
        return context;
    }

    public record CreatePackageRequest(@NotBlank String packageName,
                                       @Positive Integer totalSessions,
                                       @Positive Integer validDays,
                                       @NotNull @DecimalMin("0.01") BigDecimal price,
                                       @NotBlank String saleStatus) {
    }

    public record UpdatePackageRequest(@NotBlank String packageName,
                                       @Positive Integer totalSessions,
                                       @Positive Integer validDays,
                                       @NotNull @DecimalMin("0.01") BigDecimal price,
                                       @NotBlank String saleStatus) {
    }
}
