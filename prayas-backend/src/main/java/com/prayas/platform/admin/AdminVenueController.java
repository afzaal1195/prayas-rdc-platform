package com.prayas.platform.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lead / Faculty In-charge only -- see AccessService.canAdmin. */
@RestController
@RequestMapping("/api/v1/admin/venues")
@PreAuthorize("@access.canAdmin()")
public class AdminVenueController {

    private final AdminVenueService service;

    public AdminVenueController(AdminVenueService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminVenueView> list() {
        return service.list();
    }

    @PostMapping
    public AdminVenueView create(@Valid @RequestBody AdminVenueRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    public AdminVenueView update(@PathVariable Long id, @Valid @RequestBody AdminVenueRequest request) {
        return service.update(id, request);
    }
}
