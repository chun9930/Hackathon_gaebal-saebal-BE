package com.mcm.privatecircle.product.entity;

import java.math.BigDecimal;

import com.mcm.privatecircle.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_code", nullable = false, unique = true, length = 60)
	private String productCode;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 100)
	private String category;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "dpp_id", length = 100)
	private String dppId;

	@Column(name = "is_recommendable", nullable = false)
	private boolean recommendable = true;

	protected Product() {
	}

	public Product(
		String productCode,
		String name,
		String category,
		String imageUrl,
		BigDecimal price,
		String dppId,
		boolean recommendable
	) {
		this.productCode = productCode;
		this.name = name;
		this.category = category;
		this.imageUrl = imageUrl;
		this.price = price;
		this.dppId = dppId;
		this.recommendable = recommendable;
	}

	public Long getId() {
		return id;
	}

	public String getProductCode() {
		return productCode;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getDppId() {
		return dppId;
	}

	public boolean isRecommendable() {
		return recommendable;
	}

	public void update(
		String productCode,
		String name,
		String category,
		String imageUrl,
		BigDecimal price,
		String dppId,
		Boolean recommendable
	) {
		if (productCode != null) {
			this.productCode = productCode;
		}
		if (name != null) {
			this.name = name;
		}
		if (category != null) {
			this.category = category;
		}
		if (imageUrl != null) {
			this.imageUrl = imageUrl;
		}
		if (price != null) {
			this.price = price;
		}
		if (dppId != null) {
			this.dppId = dppId;
		}
		if (recommendable != null) {
			this.recommendable = recommendable;
		}
	}
}
