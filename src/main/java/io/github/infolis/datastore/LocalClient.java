package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.BadRequestException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class LocalClient extends AbstractClient {

    private static final Logger log = LoggerFactory.getLogger(LocalClient.class);
    public static final Map<UUID, Map<String, String>> _jsonDB = new HashMap<>();
    private final static Object lock = new Object();
    private final AtomicInteger counter = new AtomicInteger();

    public final Multimap<String, String> endpointForId = HashMultimap.<String,String> create();

    private final UUID _storeId;

    public LocalClient(UUID uuid) {
        this._storeId = uuid;
        newJsonDb();
    }

    public void newJsonDb() {
        synchronized(lock) {
            _jsonDB.put(_storeId, new HashMap<String, String>());
        }
    }

    public Map<String, String> jsonDB() {
        synchronized(lock) {
            return _jsonDB.get(_storeId);
        }
    }

    private <T extends BaseModel> void store(Class<T> clazz, T thing) {
        try {
            jsonDB().put(thing.getUri(), SerializationUtils.jacksonMapper.writeValueAsString(thing));
            String endpoint = CentralClient.getEndpointForClass(clazz);
            endpointForId.put(endpoint, thing.getUri());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends BaseModel> T get(Class<T> clazz, String id)
            throws BadRequestException {
        try {
            String json = jsonDB().get(id);
            if (null == json) {
                return null;
            }
            T readValue = SerializationUtils.jacksonMapper.readValue(json, clazz);
            readValue.setUri(id);
            return readValue;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends BaseModel> void put(Class<T> clazz, T thing)
            throws BadRequestException {
        store(clazz, thing);
    }

    @Override
    public <T extends BaseModel> void post(Class<T> clazz, T thing) {
        String id = String.format("thing_%s", counter.getAndIncrement());
        log.debug(id);
        thing.setUri(id);
        store(clazz, thing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BaseModel> List<T> search(Class<T> clazz, Multimap<String, String> query) {
        String endpoint = CentralClient.getEndpointForClass(clazz);
        List<String> ids = new ArrayList<>();
        for (String id : endpointForId.get(endpoint)) {
            String json = jsonDB().get(id);
//            log.debug("{} -> {}", json);
            Map<String, Object> obj = null;
            try {
                obj = SerializationUtils.jacksonMapper.readValue(json, Map.class);
            } catch (Exception e) {
                log.debug("{}", jsonDB());
                log.debug("{}", id);
                log.debug("{}", jsonDB().get(id));
                throw new RuntimeException(e);
            }

            // TODO make this a boolean AND instead of an OR?
            for (String searchKey : query.keySet()) {
                for (String searchValue : query.get(searchKey)) {
                    for (Object key : obj.keySet()) {
                        if (!key.equals(searchKey)) {
                            continue;
                        }
                        Object value = obj.get(key);
                        if (Collection.class.isAssignableFrom(value.getClass())) {
                            for (String subValue : ((Collection<String>) value)) {
                                if (subValue.equals(searchValue)) {
                                    ids.add(id);
                                    break;
                                }
                            }
                        } else if (value.toString().equals(searchValue)) {
                            ids.add(id);
                        }
                    }
                }
            }
        }
        return get(clazz, ids);
    }

    @Override
    public void clear() {
        jsonDB().clear();
    }

    @Override
    public void dump(Path directory, String basename) {

        /*
         * { "file": { "1234": {"filename":"foo"} }, "execution": { "5678":
         * {"algorithm":"bar"}, } }
         */

        Map<String, List<String>> dumpByEndpoint = new HashMap<String, List<String>>();
        for (String endpoint : endpointForId.keySet()) {
            Collection<String> uris = endpointForId.get(endpoint);
            if (uris.isEmpty()) {
                continue;
            }
            List<String> entries = new ArrayList<>();
            dumpByEndpoint.put(endpoint, entries);
            for (String uri : uris) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append('"');
                sb.append(uri);
                sb.append('"');
                sb.append(": ");
                sb.append(jsonDB().get(uri));
                entries.add(sb.toString());
            }
        }
        List<String> entries = new ArrayList<>();
        for (Entry<String, List<String>> entry : dumpByEndpoint.entrySet())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("  ");
            sb.append('"');
            sb.append(entry.getKey());
            sb.append('"');
            sb.append(": ");
            sb.append("{\n  ");
            sb.append(StringUtils.join(entry.getValue(), ",\n  "));
            sb.append("\n  }");
            entries.add(sb.toString());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(StringUtils.join(entries, ",\n"));
        sb.append("\n}");
        try {
            Files.createDirectories(directory);
            Path outputFile = directory.resolve(basename + ".json");
            OutputStream os = Files.newOutputStream(outputFile);
            IOUtils.write(sb.toString(), os);
            os.close();
            log.debug("Dumped to {}", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
