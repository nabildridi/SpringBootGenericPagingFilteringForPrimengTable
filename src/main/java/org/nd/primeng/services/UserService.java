package org.nd.primeng.services;



import org.nd.primeng.dao.UserDao;
import org.nd.primeng.model.User;
import org.nd.primeng.search.PrimengRequestData;
import org.nd.primeng.search.SearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import io.github.perplexhub.rsql.RSQLSupport;

@Service
public class UserService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private SearchBuilder searchBuilder;

	public Page<User> filterUsers(String json) {
		PrimengRequestData requestData = searchBuilder.process(json, User.class, "username", "email");

		if (requestData.getRsqlQuery() == null) {
			return userDao.findAll(requestData.getPageSettings());
		} else {
			return userDao.findAll(RSQLSupport.toSpecification( requestData.getRsqlQuery() ), requestData.getPageSettings());
		}
	}

}
