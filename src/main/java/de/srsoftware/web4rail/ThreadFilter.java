package de.srsoftware.web4rail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class ThreadFilter extends ch.qos.logback.core.filter.Filter<ILoggingEvent>{
	
	private List<String> keywords = List.of();
	
	@Override
	public FilterReply decide(ILoggingEvent event) {
		for (String key : keywords)	{
			if (event.getThreadName().contains(key)) return FilterReply.ACCEPT;
		}
		return FilterReply.DENY;
	}
	
	public void setKeywords(String keywords) {
		this.keywords = Arrays.stream(keywords.split(",")).map(String::trim).collect(Collectors.toList());
	}
}
