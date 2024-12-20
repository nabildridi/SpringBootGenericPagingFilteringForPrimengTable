package org.nd.primeng.services;

import org.nd.primeng.model.User;
import org.nd.primeng.repositories.UsersRepository;
import org.nd.primeng.search.PrimengQueries;
import org.nd.primeng.search.SearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class UsersService {

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private SearchBuilder searchBuilder;

	public Page<User> processPrimengRequest(String primengRequestJson) {
		PrimengQueries queries = searchBuilder.process(primengRequestJson, User.class, "id", "username", "lastname");

		return usersRepository.findAll((Specification<User>) queries.getSpec(), queries.getPageQuery());

		// If you want to change any of the queries before the JPA execution, you can use this method
		// for example you can add a custom condition to the generated query : customQuery = queries.getRsqlQuery() + " and name=='John'"
		// return usersRepository.findAll(RSQLJPASupport.<User>toSpecification(customQuery).and(RSQLJPASupport.toSort(queries.getSortQuery())),
		// queries.getPageQuery());

	}

}
