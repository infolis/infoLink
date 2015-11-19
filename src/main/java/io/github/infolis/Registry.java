package io.github.infolis;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author kata
 *
 */
public class Registry {
	
	public static final Map<String, Class<? extends BaseModel>> entities = new HashMap<>();
	static {
		entities.put("InfolisPattern", InfolisPattern.class);//InfolisPattern.class.simpleName()
		entities.put("Entity", Entity.class);
		entities.put("InfolisFile", InfolisFile.class);
	}
	
	public static Class<? extends BaseModel> getClass(String name) {
		return entities.get(name);
	}

}