package com.mcm.privatecircle.store.repository;

import com.mcm.privatecircle.store.entity.Store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
