package com.dtstack.jfilebeat.stream.transaction;

public class TransactionToken {
	
	private String rule;
	private TransactionToken previous;
	private TransactionToken next;

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public TransactionToken getPrevious() {
		return previous;
	}

	public void setPrevious(TransactionToken previous) {
		this.previous = previous;
	}

	public TransactionToken getNext() {
		return next;
	}

	public void setNext(TransactionToken next) {
		this.next = next;
	}

	public TransactionToken(String rule) {
		this.rule = rule;
	}

	@Override
	public String toString() {
		return "TransactionToken [rule=" + rule + ", next=" + next + "]";
	}
	
}
