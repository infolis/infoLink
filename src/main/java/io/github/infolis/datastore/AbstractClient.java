package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

public abstract class AbstractClient implements DataStoreClient{

	public AbstractClient() {
		super();
	}

	@Override
	public <T extends BaseModel> List<T> get(Class<T> clazz, Iterable<String> uriStrList) throws BadRequestException, ProcessingException {
		List<T> ret = new ArrayList<>();
		for (String uriStr : uriStrList)
		{
			ret.add(get(clazz, uriStr));
		}
		return ret;
	}

}