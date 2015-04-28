package com.fluidnotions.springbatch.iimport.extended;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.batch.item.file.LineMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToTupleLineMapper implements LineMapper<Tuple> {
	
    private Converter<byte[], Tuple> converter = new Converter<byte[], Tuple>(){
    	
    	private final ObjectMapper mapper = new ObjectMapper();

    	private final Converter<JsonNode, Tuple> jsonNodeToTupleConverter = new Converter<JsonNode, Tuple>(){
    		
    		@Override
    		public Tuple convert(JsonNode root) {
    			TupleBuilder builder = TupleBuilder.tuple();
    			
    			 List<String> names = new ArrayList<String>();
    			 List<Object> values = new ArrayList<Object>();
    			 
    			if (root.isValueNode()) {
    				return builder.of("value", root.asText());
    			}
    			try {
    				for (Iterator<Entry<String, JsonNode>> it = root.fields(); it.hasNext();) {
    					Entry<String, JsonNode> entry = it.next();
    					String name = entry.getKey();
    					JsonNode node = entry.getValue();
    					if (node.isObject()) {
    						// tuple
    						//builder.addEntry(name, convert(node));
    						names.add(name);
    						values.add(convert(node));
    					}
    					else if (node.isArray()) {
    						//builder.addEntry(name, nodeToList(node));
    						names.add(name);
    						values.add(nodeToList(node));
    					}
    					else {
    						if (name.equals("id")) {
    							// TODO how should this be handled?
    						}
    						else if (name.equals("timestamp")) {
    							// TODO how should this be handled?
    						}
    						else {
    							//builder.addEntry(name, node.asText());
    							names.add(name);
        						values.add(node.asText().isEmpty()?null:node.asText());
    						}
    					}
    				}
    			}
    			catch (Exception e) {
    				throw new RuntimeException(e);
    			}
    			//return builder.build();
    			return builder.ofNamesAndValues(names, values);
    		}
    		
    		private List<Object> nodeToList(JsonNode node) {
    			List<Object> list = new ArrayList<Object>(node.size());
    			for (int i = 0; i < node.size(); i++) {
    				JsonNode item = node.get(i);
    				if (item.isObject()) {
    					list.add(convert(item));
    				}
    				else if (item.isArray()) {
    					list.add(nodeToList(item));
    				}
    				else {
    					list.add(item.asText());
    				}
    			}
    			return list;
    		}
    		
    	};


    	@Override
    	public Tuple convert(byte[] source) {
    		if (source == null) {
    			return null;
    		}
    		try {
    			return jsonNodeToTupleConverter.convert(mapper.readTree(source));
    		}
    		catch (Exception e) {
    			throw new IllegalArgumentException(e.getMessage(), e);
    		}
    	}	
    };

    @Override
    public Tuple mapLine(String line, int lineNumber) throws Exception {
        return converter.convert(line.getBytes());
    }
    
   
}
