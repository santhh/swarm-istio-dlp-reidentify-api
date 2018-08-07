package com.google.swarm.istio.dlp.reidentify.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.FieldId;
import com.google.privacy.dlp.v2.ProjectName;
import com.google.privacy.dlp.v2.ReidentifyContentRequest;
import com.google.privacy.dlp.v2.ReidentifyContentResponse;
import com.google.privacy.dlp.v2.Table;
import com.google.swarm.istio.dlp.reidentify.entity.Customers;
import com.google.swarm.istio.dlp.reidentify.repo.CustomerRepository;

@RestController
@CrossOrigin
@RequestMapping("/jpa/v1/customers")
public class CustomerController {

	private final Logger LOG = LoggerFactory
			.getLogger(CustomerController.class);

	@Value("${project.name}")
	String projectName;
	@Value("${template.name}")
	String templateName;

	@Autowired
	private CustomerRepository repository;
	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Customers> getCustomers() {

		Random r = new Random();
		IntStream randomCustomer = r.ints(1, 100000);
		Integer id = randomCustomer.findFirst().getAsInt();
		Optional<Customers> customer = this.repository.findById(id.intValue());

		if (customer == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {

			List<FieldId> headers = new ArrayList<FieldId>();
			headers.add(FieldId.newBuilder().setName("SIN").build());
			headers.add(FieldId.newBuilder().setName("AccountNumber").build());
			headers.add(FieldId.newBuilder().setName("CreditCard").build());
			headers.add(FieldId.newBuilder().setName("PhoneNumber").build());
			List<Table.Row> rows = new ArrayList<>();
			Table.Row.Builder tableRowBuilder = Table.Row.newBuilder();

			tableRowBuilder
					.addValues(com.google.privacy.dlp.v2.Value.newBuilder()
							.setStringValue(customer.get().getSin()).build());

			tableRowBuilder.addValues(com.google.privacy.dlp.v2.Value
					.newBuilder().setStringValue(customer.get().getAcctNum())
					.build());

			tableRowBuilder
					.addValues(com.google.privacy.dlp.v2.Value.newBuilder()
							.setStringValue(
									customer.get().getCreditCardNumber())
							.build());

			tableRowBuilder.addValues(com.google.privacy.dlp.v2.Value
					.newBuilder()
					.setStringValue(customer.get().getPhoneNumber()).build());

			rows.add(tableRowBuilder.build());

			Table encryptedData = Table.newBuilder().addAllHeaders(headers)
					.addAllRows(rows).build();

			ContentItem tableItem = ContentItem.newBuilder()
					.setTable(encryptedData).build();

			LOG.info("Project Name: " + projectName + " Template Name: "
					+ templateName);

			ReidentifyContentRequest request = ReidentifyContentRequest
					.newBuilder()
					.setParent(ProjectName.of(projectName).toString())
					.setItem(tableItem).setReidentifyTemplateName(templateName)
					.build();

			ReidentifyContentResponse response = dlpServiceClient
					.reidentifyContent(request);

			List<Table.Row> outputRows = response.getItem().getTable()
					.getRowsList();

			customer.get()
					.setSin(outputRows.get(0).getValues(0).getStringValue());
			customer.get().setAcctNum(
					outputRows.get(0).getValues(1).getStringValue());
			customer.get().setCreditCardNumber(
					outputRows.get(0).getValues(2).getStringValue());
			customer.get().setPhoneNumber(
					outputRows.get(0).getValues(3).getStringValue());

		} catch (IOException e) {

			LOG.error(e.getMessage());
		}

		LOG.info("Customer Id: " + customer.get().getId());
		return new ResponseEntity<Customers>(customer.get(), HttpStatus.OK);
	}

}
