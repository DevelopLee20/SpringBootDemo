package com.example.jpa_service;

import org.springframework.data.repository.CrudRepository;

// Long 타입: 레디스와 JPA 데이터베이스에서는 모두 Long 타입의 고유 키 값/식별자를 사용함
public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
}
