package com.barinventory.admin.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.BarProductPrice;
import com.barinventory.admin.entity.Product;
import com.barinventory.admin.repository.BarProductPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PricingService {
    
    private final BarProductPriceRepository priceRepository;
    private final BarService barService;
    private final ProductService productService;
    
    public List<BarProductPrice> getPricesByBar(Long barId) {
        return priceRepository.findByBarBarIdAndActiveTrue(barId);
    }
    
    public BarProductPrice getPrice(Long barId, Long productId) {
        return priceRepository.findByBarBarIdAndProductProductId(barId, productId)
            .orElseThrow(() -> new RuntimeException("Price not configured for this product"));
    }
    
    @Transactional
    public BarProductPrice setPrice(Long barId, Long productId, BarProductPrice priceDetails) {
        Bar bar = barService.getBarById(barId);
        Product product = productService.getProductById(productId);
        
        BarProductPrice price = priceRepository
            .findByBarBarIdAndProductProductId(barId, productId)
            .orElse(BarProductPrice.builder()
                .bar(bar)
                .product(product)
                .build());
        
        price.setSellingPrice(priceDetails.getSellingPrice());
        price.setCostPrice(priceDetails.getCostPrice());
        price.setActive(true);
        
        return priceRepository.save(price);
    }
    
    @Transactional
    public void deactivatePrice(Long priceId) {
        BarProductPrice price = priceRepository.findById(priceId)
            .orElseThrow(() -> new RuntimeException("Price not found"));
        price.setActive(false);
        priceRepository.save(price);
    }
    
    public Map<Long, BarProductPrice> getPriceMapForBar(Long barId) {

        List<BarProductPrice> prices = 
        		priceRepository.findByBarBarIdAndActiveTrue(barId);

        return prices.stream()
                .collect(Collectors.toMap(
                        price -> price.getProduct().getProductId(),  // KEY
                        price -> price                               // VALUE
                ));
    }
    
 // ── ADD this method — alias used by the onboarding wizard controller ──
    public List<BarProductPrice> getPricesForBar(Long barId) {
        return priceRepository.findByBarBarIdAndActiveTrue(barId);
    }

    // ── ADD this method — saves pricing form submitted during onboarding ──
    @Transactional
    public void savePricesFromForm(Long barId, Map<String, String> formData) {
        Bar bar = barService.getBarById(barId);
        List<Product> products = productService.getAllActiveProducts();

        for (Product product : products) {
            String sellKey = "sell_" + product.getProductId();
            String costKey = "cost_" + product.getProductId();

            String sellVal = formData.get(sellKey);
            String costVal = formData.get(costKey);

            // Skip products where admin left selling price blank
            if (sellVal == null || sellVal.isBlank()) continue;

            BigDecimal sellingPrice = new BigDecimal(sellVal);
            BigDecimal costPrice = (costVal != null && !costVal.isBlank())
                    ? new BigDecimal(costVal)
                    : null;

            // Upsert — update if exists, create if not
            BarProductPrice price = priceRepository
                    .findByBarBarIdAndProductProductId(barId, product.getProductId())
                    .orElse(BarProductPrice.builder()
                            .bar(bar)
                            .product(product)
                            .build());

            price.setSellingPrice(sellingPrice);
            price.setCostPrice(costPrice);
            price.setActive(true);
            priceRepository.save(price);
        }
    }
    
}
