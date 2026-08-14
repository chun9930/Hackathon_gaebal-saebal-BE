package com.mcm.privatecircle.store.entity;

import com.mcm.privatecircle.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 255)
	private String location;

	protected Store() {
	}

	public Store(String name, String location) {
		this.name = name;
		this.location = location;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public void update(String name, String location) {
		if (name != null) {
			this.name = name;
		}
		if (location != null) {
			this.location = location;
		}
	}
}
