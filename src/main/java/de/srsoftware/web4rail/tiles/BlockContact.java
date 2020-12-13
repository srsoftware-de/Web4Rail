package de.srsoftware.web4rail.tiles;

import de.srsoftware.web4rail.Window;

public class BlockContact extends Contact {
	
	public BlockContact(Block parent) {
		parent(parent);
	}
	
	@Override
	public Contact addr(int address) {
		super.addr(address);
		Block block = (Block) parent(); 
		return block.register(this);		
	}

	@Override
	public Id id() {
		if (id == null) id = new Id();
		return id;
	}
	
	@Override
	public Window properties() {
		return parent().properties();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+addr+")";
	}
}
