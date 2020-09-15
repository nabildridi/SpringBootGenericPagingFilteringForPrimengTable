package org.nd.primeng.services;



import org.nd.primeng.dao.UsersDao;
import org.nd.primeng.model.User;
import org.nd.primeng.search.PrimengQueries;
import org.nd.primeng.search.SearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import io.github.perplexhub.rsql.RSQLSupport;

@Service
public class UsersService {

	@Autowired
	private UsersDao usersDao;

	@Autowired
	private SearchBuilder searchBuilder;

	public Page<User> processPrimengRequest(String primengRequestJson) {
		PrimengQueries queries = searchBuilder.process(primengRequestJson, User.class, "username", "email");

		if (queries.getRsqlQuery() == null) {
			return usersDao.findAll(RSQLSupport.toSort(queries.getSortQuery()), queries.getPageQuery());
		} else {
			return usersDao.findAll(RSQLSupport.<User>toSpecification(queries.getRsqlQuery()).and(RSQLSupport.toSort(queries.getSortQuery())), queries.getPageQuery());
		}
	}

}
