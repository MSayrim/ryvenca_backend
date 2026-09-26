package com.ryvenca.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByFirebaseUid(String firebaseUid);

    long countByCreatedAtAfter(java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where :q is null or lower(u.email) like concat('%', :q, '%') or lower(u.displayName) like concat('%', :q, '%')
            order by u.createdAt desc, u.id desc""")
    org.springframework.data.domain.Page<User> search(@org.springframework.data.repository.query.Param("q") String q,
                                                      org.springframework.data.domain.Pageable pageable);
}
