package com.fluidnotions.springbatch.export.extended;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.xd.tuple.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TuplePassThroughLineAggregator implements LineAggregator<Tuple> {

    /**
     * Because of the way our com.fluidnotions.springbatch.iimport.extended.TupleJdbcBatchItemWriter
     * works we have to include all column names in our json output so here we are setting them to 
     * an empty string. These are replaced with null values when the Tuples are rebuild on the other side
     * It may not be necessary to do this and might be unnecessary overhead
     */
    @Override
   	public String aggregate(Tuple item) {
       	
       	Converter<Tuple, String> converter = new Converter<Tuple, String>(){

       		private final ObjectMapper mapper = new ObjectMapper();

       		@Override
       		public String convert(Tuple source) {
       			ObjectNode root = toObjectNode(source);
       			String json = null;
       			try {
       				json = mapper.writeValueAsString(root);
       			}
       			catch (Exception e) {
       				throw new IllegalArgumentException("Tuple to string conversion failed", e);
       			}
       			return json;
       		}

       		private ObjectNode toObjectNode(Tuple source) {
       			ObjectNode root = mapper.createObjectNode();
       			root.put("id", source.getId().toString());
       			root.put("timestamp", source.getTimestamp());
       			for (int i = 0; i < source.size(); i++) {
       				Object value = source.getValues().get(i);
       				String name = source.getFieldNames().get(i);
       				if (value != null) {
       					// System.out.print("parsing " + name + " as ");
       					if (value instanceof Tuple) {
       						// System.out.println("tuple");
       						root.put(name, toObjectNode((Tuple) value));
       					}
       					else if (!value.getClass().isPrimitive()) {
       						// System.out.println("pojo " + value.getClass().getName());
       						root.put(name, root.pojoNode(value));
       					}
       					else {
       						// System.out.println("primitive");
       						root.put(name, value.toString());
       					}
       				}else{
       					root.put(name, "");
       				}
       			}
       			return root;
       		}
       		
       	};
       	
   		return converter.convert(item);
   	}

}
