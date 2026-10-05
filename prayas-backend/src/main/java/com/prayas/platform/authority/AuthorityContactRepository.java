package com.prayas.platform.authority;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthorityContactRepository extends JpaRepository<AuthorityContact, Long> {
    List<AuthorityContact> findAllByOrderByNameAsc();
}
