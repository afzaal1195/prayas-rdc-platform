package com.prayas.platform.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lead / Faculty In-charge only -- see AccessService.canAdmin. */
@RestController
@RequestMapping("/api/v1/admin/authority-contacts")
@PreAuthorize("@access.canAdmin()")
public class AdminAuthorityController {

    private final AdminAuthorityService service;

    public AdminAuthorityController(AdminAuthorityService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuthorityView> list() {
        return service.list();
    }

    @PostMapping
    public AuthorityView create(@Valid @RequestBody AuthorityRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}")
    public AuthorityView update(@PathVariable Long id, @Valid @RequestBody AuthorityRequest request) {
        return service.update(id, request);
    }
}
