package com.google.swarm.istio.dlp.reidentify.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Customers")
@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Customers {

	@Id
	private Integer id;
	private String userId;
	private String password;
	private String phoneNumber;
	private String creditCardNumber;
	private String sin;
	private String acctNum;

}
