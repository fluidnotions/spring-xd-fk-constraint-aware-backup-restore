package com.fluidnotions.springbatch.iimport.extended;

import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.PeekableItemReader;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.policy.CompletionPolicySupport;
import org.springframework.xd.tuple.Tuple;

public class ChunckSizeOrEOFCompletionPolicy extends CompletionPolicySupport {
	private EOFCompletionContext cc;
	private PeekableItemReader<Tuple> reader;
	private int chunkSize = 0;
	private Tuple last;

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public void setReader(PeekableItemReader<Tuple> forseeingReader) {
		this.reader = forseeingReader;
	}

	@Override
	public boolean isComplete(RepeatContext context) {
		return this.cc.isComplete();
	}

	@Override
	public RepeatContext start(RepeatContext context) {
		this.cc = new EOFCompletionContext(context);
		return cc;
	}

	class EOFCompletionContext extends RepeatContextSupport {
		
		public EOFCompletionContext(RepeatContext parent) {
			super(parent);
		}

		public void update() {
			increment();
		}

		public boolean isComplete() {
			final Tuple next;
			try {
				next = reader.peek();
			} catch (Exception e) {
				throw new NonTransientResourceException("Unable to peek", e);
			}
			
			// EOF?
			return  (getStartedCount() >= chunkSize) || (next == null);
		}

	}
}
