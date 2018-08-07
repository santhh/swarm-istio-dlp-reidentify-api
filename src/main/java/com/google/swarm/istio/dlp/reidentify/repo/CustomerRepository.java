package com.google.swarm.istio.dlp.reidentify.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.google.swarm.istio.dlp.reidentify.entity.Customers;

public interface CustomerRepository extends JpaRepository<Customers, Integer> {

}
