package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClient implements DataStoreClient{

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(AbstractClient.class);

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

	@Override
	public <T extends BaseModel> List<String> post(Class<T> clazz, Iterable<T> thingList) throws BadRequestException {
		ArrayList<String> ret = new ArrayList<>();
		for (Iterator<T> iterator = thingList.iterator(); iterator.hasNext();) {
			T thing = iterator.next();
			post(clazz, (T) thing);
			ret.add(thing.getUri());
		}
		return ret;
	}

}