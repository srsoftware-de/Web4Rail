package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;

public class BlockContact extends Contact {
	
	public BlockContact(Block parent) {
		parent(parent);
	}
	
	@Override
	public Contact addr(int address) {
		super.addr(address);
		Block block = (Block) parent();
		block.removeContact(this);
		if (address != 0) block.register(this).register();
		return this;		
	}

	@Override
	public Id id() {
		Block block = ((Block)parent());
		return new Id(block.name+":"+block.indexOf(this));
	}
	
	@Override
	public Window properties() {
		return parent().properties();
	}
	
	@Override
	public Route route() {
		return ((Block)parent()).route();
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		return ((Block)parent()).tag(replacements);
	}

	@Override
	public String toString() {
		return id().toString()+" ("+addr+")";
	}
}
