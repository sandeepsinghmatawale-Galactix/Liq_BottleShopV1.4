package com.barinventory.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.admin.config.WellConfig;
import com.barinventory.admin.entity.Bar;
import com.barinventory.admin.entity.InventorySession;
import com.barinventory.admin.entity.InventorySessionDTO;
import com.barinventory.admin.entity.Product;
import com.barinventory.admin.entity.Role;
import com.barinventory.admin.entity.User;
import com.barinventory.admin.repository.BarRepository;
import com.barinventory.admin.repository.BarWellRepository;
import com.barinventory.admin.repository.InventorySessionRepository;
import com.barinventory.admin.repository.UserRepository;
import com.barinventory.admin.service.BarService;
import com.barinventory.admin.service.InventorySessionService;
import com.barinventory.admin.service.PricingService;
import com.barinventory.admin.service.ProductService;
import com.barinventory.admin.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor

public class WebController {

	private final BarService barService;
	private final ProductService productService;
	private final PricingService pricingService;
	private final InventorySessionService sessionService;
	private final ReportService reportService;
	private final InventorySessionRepository sessionRepo;
	private final UserRepository userRepository;
	private final BarRepository barRepository;
	private final BarWellRepository barWellRepository;

	// ================= LOGIN =================

	@GetMapping("/login")
	public String showLogin() {
		return "login";
	}

	// ================= ROOT REDIRECT =================

	@GetMapping("/")
	public String redirectToDashboard(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()) {
			return "redirect:/dashboard";
		}
		return "redirect:/login";
	}

	@GetMapping("/dashboard")
	public String showDashboard(@AuthenticationPrincipal User currentUser, Model model) {

		// Safety check (should not happen if secured properly)
		if (currentUser == null) {
			return "redirect:/login";
		}

		model.addAttribute("user", currentUser);
		model.addAttribute("username", currentUser.getName());
		model.addAttribute("role", currentUser.getRole());

		// ================= ADMIN =================
		if (currentUser.getRole() == Role.ADMIN) {

			List<Bar> bars = barRepository.findAll();

			model.addAttribute("bars", bars);
			model.addAttribute("totalBars", bars.size());
			model.addAttribute("totalUsers", userRepository.count());

			return "dashboard";
		}

		// ================= BAR OWNER / STAFF =================
		Long barId = currentUser.getBarId();

		if (barId == null) {
			// If non-admin has no bar assigned → block access safely
			return "redirect:/login?error=bar_not_assigned";
		}

		Bar bar = barRepository.findById(barId).orElse(null);

		if (bar == null) {
			return "redirect:/login?error=invalid_bar";
		}

		model.addAttribute("bar", bar);
		model.addAttribute("barId", barId);

		// Only BAR_OWNER sees staff count
		if (currentUser.getRole() == Role.BAR_OWNER) {
			long staffCount = userRepository.countByBar_BarIdAndRole(barId, Role.BAR_STAFF);
			model.addAttribute("staffCount", staffCount);
		}

		return "dashboard";
	}

	// ═══════════════════════════════════════════════════
//  BAR ONBOARDING WIZARD
// ═══════════════════════════════════════════════════

// STEP 1a — Show registration form
	@GetMapping("/admin/bars/register")
	public String showRegisterBar(Model model) {
		model.addAttribute("barTypes", List.of("STANDALONE", "HOTEL_BAR", "RESTAURANT_BAR", "CLUB", "OTHER"));
		model.addAttribute("shiftConfigs", List.of("SINGLE", "DOUBLE"));
		return "admin/bar-register";
	}

// STEP 1b — Save basic info → go to well config
	@PostMapping("/admin/bars/register")
	public String saveRegisterBar(@RequestParam Map<String, String> form) {
		Bar bar = Bar.builder().barName(form.get("barName")).barType(form.get("barType"))
				.ownerName(form.get("ownerName")).contactNumber(form.get("contactNumber")).email(form.get("email"))
				.addressLine(form.get("addressLine")).city(form.get("city")).state(form.get("state"))
				.pinCode(form.get("pinCode")).licenseNumber(form.get("licenseNumber"))
				.licenseType(form.get("licenseType"))
				.licenseExpiryDate(form.get("licenseExpiryDate") != null && !form.get("licenseExpiryDate").isEmpty()
						? LocalDate.parse(form.get("licenseExpiryDate"))
						: null)
				.gstin(form.get("gstin")).shiftConfig(form.get("shiftConfig"))
				.openingDate(form.get("openingDate") != null && !form.get("openingDate").isEmpty()
						? LocalDate.parse(form.get("openingDate"))
						: null)
				.active(false).setupComplete(false).onboardingStep("WELL_CONFIG").build();

		Bar saved = barService.createBar(bar);
		return "redirect:/admin/bars/" + saved.getBarId() + "/wells-config";
	}

// STEP 2a — Well configuration
	@GetMapping("/admin/bars/{barId}/wells-config")
	public String showWellsConfig(@PathVariable Long barId, Model model) {
		model.addAttribute("bar", barService.getBarById(barId));
		model.addAttribute("existingWells", barWellRepository.findByBarBarIdAndActiveTrue(barId));
		return "admin/bar-wells-config";
	}

// STEP 2b — Save wells
	@PostMapping("/admin/bars/{barId}/wells-config")
	public String saveWellsConfig(@PathVariable Long barId, @RequestParam Map<String, String> form) {
		barService.saveWellsConfig(barId, form);
		barService.updateOnboardingStep(barId, "PRICING");
		return "redirect:/admin/bars/" + barId + "/pricing";
	}

// STEP 3a — Product pricing
	@GetMapping("/admin/bars/{barId}/pricing")
	public String showPricing(@PathVariable Long barId, Model model) {
		model.addAttribute("bar", barService.getBarById(barId));
		model.addAttribute("products", productService.getAllActiveProducts());
		model.addAttribute("existingPrices", pricingService.getPricesForBar(barId));
		return "admin/bar-pricing";
	}

// STEP 3b — Save pricing
	@PostMapping("/admin/bars/{barId}/pricing")
	public String savePricing(@PathVariable Long barId, @RequestParam Map<String, String> form) {
		pricingService.savePricesFromForm(barId, form);
		barService.updateOnboardingStep(barId, "OPENING_STOCK");
		return "redirect:/admin/bars/" + barId + "/setup";
	}

// STEP 4 — Opening stock (your existing setup flow takes over from here)
	@GetMapping("/admin/bars/{barId}/setup")
	public String showSetupLanding(@PathVariable Long barId, Model model) {
		model.addAttribute("bar", barService.getBarById(barId));
		Optional<InventorySession> existing = sessionService.getSetupSession(barId);
		model.addAttribute("setupSession", existing.orElse(null));
		return "admin/setup-landing";
	}

// STEP 5 — Activate bar (called from setup confirm/finalize)
	@PostMapping("/admin/bars/{barId}/activate")
	public String activateBar(@PathVariable Long barId) {
		barService.activateBar(barId);
		return "redirect:/dashboard?activated=" + barId;
	}

	// ================= PRODUCTS =================

	@GetMapping("/products")
	public String listProducts(Model model) {
		model.addAttribute("products", productService.getAllActiveProducts());
		return "list";
	}

	@GetMapping("/products/new")
	public String newProductForm() {
		return "productNew";
	}

	@PostMapping("/products/new")
	public String createProduct(@RequestParam String productName, @RequestParam String category,
			@RequestParam(required = false) String brand, @RequestParam(required = false) String volumeML,
			@RequestParam String unit) {

		Product product = Product.builder().productName(productName).category(category).brand(brand)
				.volumeML(volumeML != null && !volumeML.isEmpty() ? new BigDecimal(volumeML) : null).unit(unit)
				.active(true).build();

		productService.createProduct(product);
		return "redirect:/products";
	}

	// ================= REPORTS =================

	@GetMapping("/reports/{barId}/daily")
	public String dailyReport(@PathVariable Long barId, @RequestParam(required = false) String date, Model model) {

		LocalDateTime reportDate = date != null ? LocalDateTime.parse(date) : LocalDateTime.now();

		model.addAttribute("bar", barService.getBarById(barId));
		model.addAttribute("report", reportService.getDailySalesReport(barId, reportDate));

		return "reports/daily";
	}

	// ================= REST INITIALIZE =================

	@PostMapping("/initialize")
	public ResponseEntity<InventorySessionDTO> initializeSession(@RequestParam Long barId,
			@RequestParam String shiftType, @RequestParam String notes) {

		InventorySession inv = sessionService.initializeSession(barId, shiftType, notes);

		InventorySessionDTO dto = InventorySessionDTO.builder().sessionId(inv.getSessionId())
				.barId(inv.getBar().getBarId()).barName(inv.getBar().getBarName())
				.sessionStartTime(inv.getSessionStartTime()).status(inv.getStatus()).shiftType(inv.getShiftType())
				.notes(inv.getNotes()).build();

		return ResponseEntity.ok(dto);
	}

	// ================= ADMIN BAR SETUP =================

	@PostMapping("/admin/bars/{barId}/setup/start")
	public String startSetup(@PathVariable Long barId) {
		InventorySession session = sessionService.createSetupSession(barId);
		return "redirect:/admin/setup/" + session.getSessionId() + "/stockroom";
	}

	@GetMapping("/admin/setup/{sessionId}/stockroom")
	public String showSetupStockroom(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId); // ← add this
		model.addAttribute("products", productService.getAllActiveProducts());
		Map<Long, BigDecimal> existing = sessionService.getSetupStockroomData(sessionId);
		model.addAttribute("existingStock", existing);
		return "admin/setup-stockroom";
	}

	@PostMapping("/admin/setup/{sessionId}/stockroom")
	public String saveSetupStockroom(@PathVariable Long sessionId, @RequestParam Map<String, String> formData,
			Model model) {
		try {
			sessionService.saveSetupStockroom(sessionId, formData);
			return "redirect:/admin/setup/" + sessionId + "/wells";
		} catch (Exception e) {
			// log.error("Error saving setup stockroom: {}", e.getMessage());
			InventorySession session = sessionService.getSession(sessionId);
			model.addAttribute("session", session);
			model.addAttribute("products", productService.getAllActiveProducts());
			model.addAttribute("existingStock", sessionService.getSetupStockroomData(sessionId));
			model.addAttribute("error", e.getMessage());
			return "admin/setup-stockroom";
		}
	}

	@GetMapping("/admin/setup/{sessionId}/wells")
	public String showSetupWells(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId); // ← ADD THIS
		model.addAttribute("products", productService.getAllActiveProducts());
		List<String> wellNames = sessionService.getWellNamesForBar(session.getBar().getBarId());
		model.addAttribute("wellNames", wellNames);
		Map<String, BigDecimal> existing = sessionService.getSetupWellsData(sessionId);
		model.addAttribute("existingStock", existing);
		return "admin/setup-wells";
	}

	@GetMapping("/admin/setup/{sessionId}/confirm")
	public String showSetupConfirm(@PathVariable Long sessionId, Model model) {
		InventorySession session = sessionService.getSession(sessionId);
		model.addAttribute("session", session);
		model.addAttribute("sessionId", sessionId); // ← ADD THIS
		model.addAttribute("stockroomData", sessionService.getSetupStockroomData(sessionId));
		model.addAttribute("wellsData", sessionService.getSetupWellsData(sessionId));
		model.addAttribute("products", productService.getAllActiveProducts());
		model.addAttribute("wellNames", sessionService.getWellNamesForBar(session.getBar().getBarId())); // ← also use
																											// dynamic
																											// wells
																											// here
		return "admin/setup-confirm";
	}

	@PostMapping("/admin/setup/{sessionId}/wells")
	public String saveSetupWells(@PathVariable Long sessionId, @RequestParam Map<String, String> formData) {
		sessionService.saveSetupWells(sessionId, formData);
		return "redirect:/admin/setup/" + sessionId + "/confirm";
	}

	@PostMapping("/admin/setup/{sessionId}/finalize")
	public String finalizeSetup(@PathVariable Long sessionId) {
		sessionService.finalizeSetupSession(sessionId);
		return "redirect:/dashboard?setupComplete=true";
	}

	// ================= HELPER =================

	private BigDecimal parseDecimal(String value) {
		return (value != null && !value.isEmpty()) ? new BigDecimal(value) : BigDecimal.ZERO;
	}
}