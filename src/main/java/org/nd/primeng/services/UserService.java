package org.nd.primeng.services;



import org.nd.primeng.dao.UserDao;
import org.nd.primeng.model.User;
import org.nd.primeng.search.PrimengRequestData;
import org.nd.primeng.search.SearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.github.perplexhub.rsql.RSQLSupport;

@Service
public class UserService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private SearchBuilder searchBuilder;

	public Page<User> filterUsers(String json) {
		PrimengRequestData requestData = searchBuilder.parse(json, User.class);

		Pageable pageSettings = searchBuilder.buildPageable(requestData);

		String rsqlQuery = null;
		if (requestData.isColumnsFiltering()) {
			rsqlQuery = searchBuilder.buildFiltersQuery(requestData);
		}
		if (requestData.isGeneralFiltering()) {
			rsqlQuery = searchBuilder.buildGlobalFilterQuery(requestData, "username", "email");
		}

		if (rsqlQuery == null) {
			return userDao.findAll(pageSettings);
		} else {
			return userDao.findAll(RSQLSupport.toSpecification(rsqlQuery), pageSettings);
		}
	}

}
