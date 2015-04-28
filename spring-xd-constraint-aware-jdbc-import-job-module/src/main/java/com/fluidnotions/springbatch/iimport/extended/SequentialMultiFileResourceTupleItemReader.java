package com.fluidnotions.springbatch.iimport.extended;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.PeekableItemReader;
import org.springframework.batch.item.ResourceAware;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.xd.tuple.Tuple;

public class SequentialMultiFileResourceTupleItemReader extends
		AbstractItemStreamItemReader<Tuple> implements
		PeekableItemReader<Tuple> {

	private static final Log logger = LogFactory
			.getLog(SequentialMultiFileResourceTupleItemReader.class);

	private static final String RESOURCE_KEY = "resourceIndex";

	private ResourceAwareItemReaderItemStream<? extends Tuple> delegate;

	private Tuple next;

	private ExecutionContext executionContext = new ExecutionContext();

	private Resource[] resources;

	private boolean saveState = true;

	private int currentResource = -1;

	// signals there are no resources to read -> just return null on first read
	private boolean noInput;

	private boolean strict = false;

	public void setResourceDirectoryPath(String resourceDirectoryPath) {
		Assert.notNull(resourceDirectoryPath,
				"The resource Directory Path must not be null");
		Assert.isTrue(new File(resourceDirectoryPath).exists(),
				"The resource Directory Path (" + resourceDirectoryPath
						+ ") must exist");
		File resDir = new File(resourceDirectoryPath);
		// list files
		String[] filenameArr = resDir.list();
		List<String> filenameList = Arrays.asList(filenameArr);
		Collections.sort(filenameList, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int numPrefix1 = new Integer(o1.substring(0, o1.indexOf("-")));
				int numPrefix2 = new Integer(o2.substring(0, o2.indexOf("-")));
				return numPrefix1 - numPrefix2;
			}

		});

		PathResource[] orderedPathArr = new PathResource[filenameList.size()];
		int i = 0;
		for (String path : filenameList) {
			logger.debug("adding new PathResource to resource list: " + path);
			orderedPathArr[i++] = new PathResource(resourceDirectoryPath
					+ File.separator + path);
		}

		setResources(orderedPathArr);
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link #open(org.springframework.batch.item.ExecutionContext)} if there
	 * are no resources to read.
	 * 
	 * @param strict
	 *            false by default
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	private Comparator<Resource> comparator = new Comparator<Resource>() {

		@Override
		public int compare(Resource r1, Resource r2) {
			String o1 = r1.getFilename();
			String o2 = r2.getFilename();
			int numPrefix1 = new Integer(o1.substring(0, o1.indexOf("-")));
			int numPrefix2 = new Integer(o2.substring(0, o2.indexOf("-")));
			return numPrefix1 - numPrefix2;
		}

	};

	public SequentialMultiFileResourceTupleItemReader() {
		this.setExecutionContextName(ClassUtils
				.getShortName(SequentialMultiFileResourceTupleItemReader.class));
	}

	@Override
	public Tuple peek() throws Exception, UnexpectedInputException,
			ParseException {
		if (next == null) {
			updateDelegate(executionContext);
			next = readFromDelegate();
		}
		return next;
	}

	@Override
	public void update(ExecutionContext executionContext)
			throws ItemStreamException {
		if (next != null) {
			// Get the last state from the delegate instead of using
			// current value.
			for (Entry<String, Object> entry : this.executionContext.entrySet()) {
				executionContext.put(entry.getKey(), entry.getValue());
			}
			return;
		}
		updateDelegate(executionContext);
	}

	private void updateDelegate(ExecutionContext executionContext) {
		super.update(executionContext);
		if (saveState) {
			executionContext.putInt(getExecutionContextKey(RESOURCE_KEY),
					currentResource);
			delegate.update(executionContext);
		}
	}

	/**
	 * Reads the next item, jumping to next resource if necessary.
	 */
	@Override
	public Tuple read() throws Exception, UnexpectedInputException,
			ParseException {

		if (next != null) {
			Tuple item = next;
			next = null;
			// executionContext = new ExecutionContext();
			return item;
		}

		if (noInput) {
			return null;
		}

		// If there is no resource, then this is the first item, set the current
		// resource to 0 and open the first delegate.
		if (currentResource == -1) {
			currentResource = 0;
			delegate.setResource(resources[currentResource]);
			delegate.open(new ExecutionContext());
		}

		return readNextItem();
	}

	/**
	 * Use the delegate to read the next item, jump to next resource if current
	 * one is exhausted. Items are appended to the buffer.
	 * 
	 * @return next item from input
	 */
	private Tuple readNextItem() throws Exception {

		Tuple item = readFromDelegate();

		while (item == null) {

			currentResource++;

			if (currentResource >= resources.length) {
				return null;
			}

			delegate.close();
			delegate.setResource(resources[currentResource]);
			delegate.open(new ExecutionContext());

			item = readFromDelegate();
		}

		return item;
	}

	private Tuple readFromDelegate() throws Exception {
		if (next != null) {
			Tuple item = next;
			next = null;
			// executionContext = new ExecutionContext();
			return item;
		}
		Tuple item = delegate.read();
		if (item instanceof ResourceAware) {
			((ResourceAware) item).setResource(getCurrentResource());
		}
		return item;
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	@Override
	public void close() throws ItemStreamException {
		super.close();
		delegate.close();
		noInput = false;
	}

	/**
	 * Figure out which resource to start with in case of restart, open the
	 * delegate and restore delegate's position in the resource.
	 */
	@Override
	public void open(ExecutionContext executionContext)
			throws ItemStreamException {
		super.open(executionContext);
		Assert.notNull(resources, "Resources must be set");

		noInput = false;
		if (resources.length == 0) {
			if (strict) {
				throw new IllegalStateException(
						"No resources to read. Set strict=false if this is not an error condition.");
			} else {
				logger.warn("No resources to read. Set strict=true if this should be an error condition.");
				noInput = true;
				return;
			}
		}

		Arrays.sort(resources, comparator);

		if (executionContext.containsKey(getExecutionContextKey(RESOURCE_KEY))) {
			currentResource = executionContext
					.getInt(getExecutionContextKey(RESOURCE_KEY));

			// context could have been saved before reading anything
			if (currentResource == -1) {
				currentResource = 0;
			}

			delegate.setResource(resources[currentResource]);
			delegate.open(executionContext);
		} else {
			currentResource = -1;
		}
	}

	/**
	 * @param delegate
	 *            reads items from single {@link Resource}.
	 */
	public void setDelegate(
			ResourceAwareItemReaderItemStream<? extends Tuple> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Set the boolean indicating whether or not state should be saved in the
	 * provided {@link ExecutionContext} during the {@link ItemStream} call to
	 * update.
	 * 
	 * @param saveState
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * @param comparator
	 *            used to order the injected resources, by default compares
	 *            {@link Resource#getFilename()} values.
	 */
	public void setComparator(Comparator<Resource> comparator) {
		this.comparator = comparator;
	}

	/**
	 * @param resources
	 *            input resources
	 */
	public void setResources(Resource[] resources) {
		Assert.notNull(resources, "Tuplehe resources must not be null");
		this.resources = Arrays.asList(resources).toArray(
				new Resource[resources.length]);
	}

	public Resource getCurrentResource() {
		if (currentResource >= resources.length || currentResource < 0) {
			return null;
		}
		return resources[currentResource];
	}

}
